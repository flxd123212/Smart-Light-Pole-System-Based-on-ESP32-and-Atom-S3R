"""
智能灯杆 — Flask 服务器 (AI 图像服务)
======================================
角色: 接收摄像头图片, 提供实时流+拍照触发+图库+YOLO代理
数据回写: 优先通过 MQTT 上报给 iot-light-main 后端写库,
          MQTT 不可用时直接写 MySQL (双通道保障)。
          主题: light/camera/CAM001/capture
"""

import io, os, time, queue, threading, base64, datetime, json
import requests
import pymysql
from flask import (
    Flask, Response, jsonify, render_template, request, send_file, stream_with_context,
)

try:
    import paho.mqtt.client as mqtt
    MQTT_LIB_OK = True
except ImportError:
    MQTT_LIB_OK = False

# ==================== MySQL 直写 (MQTT 不可用时的回退通道) ====================
DB_CONFIG = {
    "host": "127.0.0.1",
    "port": 3306,
    "user": "root",
    "password": "CHANGE_ME_LOCALLY",
    "database": "iot-light",
    "charset": "utf8mb4",
}
POLE_ID = 100


def db_insert_capture(image_url, person_count, result_json=None):
    conn = None
    try:
        conn = pymysql.connect(connect_timeout=5, **DB_CONFIG)
        with conn.cursor() as cur:
            sql = """INSERT INTO iot_camera_capture
                     (pole_id, image_url, person_count, result_json, capture_time,
                      del_flag, create_by, create_time)
                     VALUES (%s,%s,%s,%s,NOW(),'0','system',NOW())"""
            cur.execute(sql, (POLE_ID, image_url, person_count, result_json))
            conn.commit()
        print(f"[DB] ✅ 直写成功: {image_url}")
        return True
    except Exception as e:
        print(f"[DB] ❌ 写入失败: {e}")
        return False
    finally:
        # 原代码只在成功路径 close(), 异常时连接泄漏, 跑久了打满 MySQL
        if conn is not None:
            try:
                conn.close()
            except Exception:
                pass


# ==================== MQTT 配置 ====================
# EMQX 运行在 Windows 业务主机上; 虚拟机里运行时把 MQTT_HOST 改成业务主机局域网 IP
MQTT_HOST = os.environ.get("MQTT_HOST", "192.168.106.134")
MQTT_PORT = int(os.environ.get("MQTT_PORT", "1883"))
MQTT_CLIENT_ID = os.environ.get("MQTT_CLIENT_ID", "flask-ai-server")
CAMERA_CODE = os.environ.get("CAMERA_CODE", "CAM001")   # 后端自动注册的摄像头设备编码
LAMP_ID = os.environ.get("LAMP_ID", "LP001")            # 对应 iot_pole.pole_code = LP001
TOPIC_CAPTURE = f"light/camera/{CAMERA_CODE}/capture"   # 抓拍上报 (后端写入 iot_camera_capture)
TOPIC_STATUS  = f"light/camera/{CAMERA_CODE}/status"    # 摄像头状态
TOPIC_COMMAND = f"light/camera/{CAMERA_CODE}/command"   # 抓拍触发命令 (后端下发, 收到后置 trigger_flag)

YOLO_URL = "http://127.0.0.1:5001/infer"
PHOTO_DIR = os.path.join(os.path.dirname(__file__), "photos")
os.makedirs(PHOTO_DIR, exist_ok=True)

# 检测线程每 2 秒请求一次 YOLO; 用 Session 复用 keep-alive 连接,
# 否则长时间运行会堆积大量 TIME_WAIT 套接字, 最终耗尽临时端口
yolo_session = requests.Session()

app = Flask(__name__)
app.config['JSONIFY_PRETTYPRINT_REGULAR'] = False


# 前端 (RuoYi-Vue, 80/8080 端口) 与本服务不同源, 实时监控页要读检测 JSON,
# 这里统一放行跨域; 不额外引入 flask-cors 依赖, 保持零新增依赖
@app.after_request
def _add_cors_headers(resp):
    resp.headers.setdefault("Access-Control-Allow-Origin", "*")
    resp.headers.setdefault("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
    resp.headers.setdefault("Access-Control-Allow-Headers", "Content-Type, Authorization")
    return resp

# ==================== MQTT 客户端 ====================
mqtt_client = None
mqtt_connected = False


def mqtt_publish(topic, payload):
    """发布 MQTT 消息, 失败时打印日志 (不影响主流程)"""
    global mqtt_connected
    if mqtt_client is None or not mqtt_connected:
        print(f"[MQTT] 未连接, 丢弃消息 topic={topic}")
        return False
    try:
        mqtt_client.publish(topic, payload, qos=1)
        print(f"[MQTT] 发布 OK topic={topic} payload={payload[:120]}")
        return True
    except Exception as e:
        print(f"[MQTT] 发布失败: {e}")
        return False


def mqtt_loop():
    """连接 EMQX 并保持重连 (守护线程)"""
    global mqtt_client, mqtt_connected
    if not MQTT_LIB_OK:
        print("[MQTT] 未安装 paho-mqtt, 请执行: pip install paho-mqtt")
        return
    client = mqtt.Client(client_id=MQTT_CLIENT_ID, clean_session=True)
    client.reconnect_delay_set(min_delay=1, max_delay=30)

    def on_connect(c, userdata, flags, rc):
        global mqtt_connected
        if rc == 0:
            mqtt_connected = True
            print(f"[MQTT] ✅ 已连接 {MQTT_HOST}:{MQTT_PORT} rc={rc}")
            # 上报摄像头在线状态
            mqtt_publish(TOPIC_STATUS, json.dumps({"lampId": LAMP_ID, "status": "0"},
                                                  ensure_ascii=False))
            # 订阅抓拍触发命令 (后端/后台下发的拍照指令)
            c.subscribe(TOPIC_COMMAND, qos=1)
            print(f"[MQTT] 已订阅命令主题: {TOPIC_COMMAND}")
        else:
            mqtt_connected = False
            print(f"[MQTT] 连接失败 rc={rc}")

    def on_message(c, userdata, msg):
        global trigger_flag, trigger_time
        topic = msg.topic
        payload = msg.payload.decode(errors="replace")
        print(f"[MQTT] 收到命令 topic={topic} payload={payload}")
        try:
            data = json.loads(payload)
            if data.get("action") == "capture":
                with trigger_lock:
                    trigger_flag = True
                    trigger_time = time.time()
                print("[MQTT] 🎯 收到抓拍指令 → 触发拍照")
        except Exception as e:
            print(f"[MQTT] 命令解析失败: {e}")

    def on_disconnect(c, userdata, rc):
        global mqtt_connected
        mqtt_connected = False
        print(f"[MQTT] 连接断开 rc={rc}, 等待重连...")

    client.on_connect = on_connect
    client.on_disconnect = on_disconnect
    client.on_message = on_message
    mqtt_client = client
    while True:
        try:
            client.connect(MQTT_HOST, MQTT_PORT, keepalive=60)
            client.loop_forever()
        except Exception as e:
            print(f"[MQTT] 连接异常: {e}, 5秒后重试")
            time.sleep(5)


threading.Thread(target=mqtt_loop, daemon=True).start()

# ==================== 全局状态 ====================
trigger_flag = False
trigger_lock = threading.Lock()
# 最近一次置位的时间戳。必须由"每一个"置位点更新, 否则 detection_loop 会用
# 初值 0 判定 time.time()-0 > 15 成立, 在下一个 2 秒 tick 就把手动/MQTT
# 触发的标志立刻清掉, 导致中转板还没轮询到就丢了这次拍照。
trigger_time = 0.0
TRIGGER_TIMEOUT = 15.0

latest_stream = None
stream_lock = threading.Lock()
latest_photo_path = None

camera_ip = None
camera_ip_lock = threading.Lock()

sse_clients = []
sse_clients_lock = threading.Lock()

pending_ai_result = {}   # {"person_count": N, "result_json": "{...}"}
pending_ai_lock = threading.Lock()

# ==================== 实时监控 (前端画框) 状态 ====================
# 有人在看实时画面时提高检测频率, 没人看时退回省电模式, 避免空转打满 CPU
DETECT_LIVE_INTERVAL = float(os.environ.get("DETECT_LIVE_INTERVAL", "0.35"))
DETECT_IDLE_INTERVAL = float(os.environ.get("DETECT_IDLE_INTERVAL", "2.0"))
VIEWER_TTL = 8.0          # 超过 8 秒没人拉检测结果就算没人看
PERSON_CONF = float(os.environ.get("PERSON_CONF", "0.3"))

latest_detection = {
    "ts": 0.0, "boxes": [], "person_count": 0,
    "width": 640, "height": 360, "latency_ms": 0,
}
detection_lock = threading.Lock()

annotated_frame = None            # YOLO 服务端画好框的 JPEG (bytes)
annotated_ts = 0.0
annotated_lock = threading.Lock()

last_viewer_ts = 0.0              # 最近一次前端拉取实时数据的时间
last_frame_ts = 0.0               # 最近一次收到摄像头帧的时间
frame_times = []                  # 最近若干帧时间戳, 用于算上行帧率
frame_times_lock = threading.Lock()
yolo_online = False


def touch_viewer():
    """标记当前有人在看实时画面"""
    global last_viewer_ts
    last_viewer_ts = time.time()


def viewer_active():
    return (time.time() - last_viewer_ts) < VIEWER_TTL


def camera_fps():
    now = time.time()
    with frame_times_lock:
        recent = [t for t in frame_times if now - t <= 5.0]
    if len(recent) < 2:
        return 0.0
    span = recent[-1] - recent[0]
    return round((len(recent) - 1) / span, 1) if span > 0 else 0.0


def sse_broadcast(event, data):
    with sse_clients_lock:
        dead = []
        for q in sse_clients:
            try:
                q.put_nowait((event, data))
            except queue.Full:
                dead.append(q)
        for q in dead:
            sse_clients.remove(q)


@app.route("/")
def index():
    return render_template("index.html")


@app.route("/api/events")
def sse_events():
    q = queue.Queue(maxsize=16)
    with sse_clients_lock:
        sse_clients.append(q)

    def generate():
        # 原来是无超时的 q.get(): 浏览器断开后生成器再也不会被唤醒,
        # GeneratorExit 抛不出来 -> finally 不执行 -> 每次 EventSource 重连
        # 都永久泄漏一个线程和一个队列, 最终 Flask 起不了新线程, 接口全 500。
        # 改为带超时 + 心跳: 客户端已断开时写心跳会抛异常, 线程得以退出。
        try:
            while True:
                try:
                    event, data = q.get(timeout=15)
                except queue.Empty:
                    yield ": keep-alive\n\n"   # SSE 注释行, 客户端忽略
                    continue
                yield f"event: {event}\ndata: {data}\n\n"
        except (GeneratorExit, OSError, ConnectionError):
            pass
        finally:
            with sse_clients_lock:
                if q in sse_clients:
                    sse_clients.remove(q)

    return Response(
        stream_with_context(generate()),
        mimetype="text/event-stream",
        headers={"Cache-Control": "no-cache", "Connection": "keep-alive",
                 "X-Accel-Buffering": "no"},
    )


@app.route("/api/register/camera", methods=["POST"])
def register_camera():
    global camera_ip
    data = request.get_json(force=True)
    ip = data.get("ip", "")
    if ip:
        with camera_ip_lock:
            camera_ip = ip
        print(f"[相机] ✅ AtomS3R IP: {ip}")
        return jsonify({"ok": True}), 200
    return jsonify({"error": "no ip"}), 400


@app.route("/api/camera/ip")
def camera_ip_endpoint():
    with camera_ip_lock:
        if camera_ip:
            return jsonify({"ip": camera_ip})
        return jsonify({"ip": None}), 404


@app.route("/api/camera/stream_url")
def camera_stream_url():
    with camera_ip_lock:
        if camera_ip:
            return jsonify({"url": f"http://{camera_ip}/stream"})
        return jsonify({"url": None}), 404


@app.route("/api/upload/stream", methods=["POST"])
def upload_stream():
    global latest_stream, camera_ip, last_frame_ts
    data = request.get_data()
    if not data:
        return "No data", 400
    with stream_lock:
        latest_stream = bytes(data)
    now = time.time()
    last_frame_ts = now
    with frame_times_lock:
        frame_times.append(now)
        if len(frame_times) > 60:
            del frame_times[:-60]
    client_ip = request.remote_addr
    if client_ip and client_ip != camera_ip:
        with camera_ip_lock:
            camera_ip = client_ip
        print(f"[相机] ✅ 自动注册 IP: {camera_ip}")
    return jsonify({"ok": True}), 200


@app.route("/api/stream/latest")
def stream_latest():
    # 前端轮询用: 必须返回最新帧且不被浏览器缓存 (Flask 3.x send_file 无 headers 参数)
    if latest_stream is not None:
        with stream_lock:
            frame = latest_stream
        if frame is not None:
            resp = Response(frame, mimetype="image/jpeg")
            resp.headers["Cache-Control"] = "no-store, no-cache, must-revalidate"
            return resp
    resp = send_file(_placeholder_jpeg(), mimetype="image/jpeg")
    resp.headers["Cache-Control"] = "no-store, no-cache, must-revalidate"
    return resp


@app.route("/api/stream/mjpeg")
def stream_mjpeg():
    """MJPEG 实时流: 持续推送当前帧(10fps), 帧不变化也重复发送,
    保证浏览器页面一直有画面; 长时间无任何帧才断开"""
    def generate():
        idle = 0
        started = time.time()
        try:
            while True:
                # 单条连接最长 10 分钟, 浏览器会自动重连;
                # 防止异常断开的连接把线程永久占住
                if time.time() - started > 600:
                    break
                with stream_lock:
                    frame = latest_stream
                if frame is not None:
                    yield b"--frame\r\nContent-Type: image/jpeg\r\n\r\n" + frame + b"\r\n"
                    idle = 0
                    time.sleep(0.1)   # ~10fps
                else:
                    idle += 1
                    if idle > 200:    # ~20s 无帧才断开, 等待摄像头接入
                        break
                    time.sleep(0.1)
        except (GeneratorExit, OSError, ConnectionError):
            pass
    return Response(generate(), mimetype="multipart/x-mixed-replace; boundary=frame")


# ==================== 实时监控接口 (前端画框用) ====================

@app.route("/api/detect/latest")
def detect_latest():
    """最近一次 YOLO 检测结果 (归一化坐标), 前端每 300ms 拉一次并画在画面上。
    被调用即表示"有人在看", 检测线程会自动切到高频模式。"""
    touch_viewer()
    with detection_lock:
        det = dict(latest_detection)
    now = time.time()
    det["age_ms"] = int((now - det["ts"]) * 1000) if det["ts"] else -1
    det["camera_online"] = bool(last_frame_ts and (now - last_frame_ts) < 5.0)
    det["camera_fps"] = camera_fps()
    det["yolo_online"] = yolo_online
    det["mqtt_connected"] = mqtt_connected
    with camera_ip_lock:
        det["camera_ip"] = camera_ip
    resp = jsonify(det)
    resp.headers["Cache-Control"] = "no-store, no-cache, must-revalidate"
    return resp


@app.route("/api/status")
def api_status():
    """实时监控面板顶部的状态汇总"""
    touch_viewer()
    now = time.time()
    with detection_lock:
        det = dict(latest_detection)
    with camera_ip_lock:
        ip = camera_ip
    return jsonify({
        "camera_online": bool(last_frame_ts and (now - last_frame_ts) < 5.0),
        "camera_ip": ip,
        "camera_fps": camera_fps(),
        "last_frame_age_ms": int((now - last_frame_ts) * 1000) if last_frame_ts else -1,
        "yolo_online": yolo_online,
        "mqtt_connected": mqtt_connected,
        "person_count": det.get("person_count", 0),
        "detect_interval": DETECT_LIVE_INTERVAL if viewer_active() else DETECT_IDLE_INTERVAL,
        "server_time": datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
    })


@app.route("/api/stream/annotated")
def stream_annotated():
    """AI 标注流: 推送 YOLO 服务端画好框的帧 (前端 canvas 画框的兜底方案)。
    还没有标注帧时先回原始帧, 保证画面不黑屏。"""
    def generate():
        started = time.time()
        idle = 0
        try:
            while True:
                touch_viewer()
                if time.time() - started > 600:   # 单连接最长 10 分钟, 浏览器会自动重连
                    break
                with annotated_lock:
                    frame = annotated_frame
                    fresh = annotated_ts and (time.time() - annotated_ts) < 3.0
                if not fresh:
                    with stream_lock:
                        frame = latest_stream
                if frame is not None:
                    yield b"--frame\r\nContent-Type: image/jpeg\r\n\r\n" + frame + b"\r\n"
                    idle = 0
                    time.sleep(0.1)
                else:
                    idle += 1
                    if idle > 200:
                        break
                    time.sleep(0.1)
        except (GeneratorExit, OSError, ConnectionError):
            pass
    return Response(generate(), mimetype="multipart/x-mixed-replace; boundary=frame")


@app.route("/api/detect/frame")
def detect_frame():
    """最新的 AI 标注单帧 (轮询模式 / 截图用)"""
    touch_viewer()
    with annotated_lock:
        frame = annotated_frame
        fresh = annotated_ts and (time.time() - annotated_ts) < 5.0
    if frame is None or not fresh:
        with stream_lock:
            frame = latest_stream
    if frame is None:
        resp = send_file(_placeholder_jpeg(), mimetype="image/jpeg")
    else:
        resp = Response(frame, mimetype="image/jpeg")
    resp.headers["Cache-Control"] = "no-store, no-cache, must-revalidate"
    return resp


def report_capture(image_url, person_count, result_json):
    """通过 MQTT 上报抓拍结果, MQTT 不可用时直写 MySQL"""
    payload = {
        "lampId": LAMP_ID,
        "imageUrl": image_url,
        "personCount": person_count if person_count else 0,
        "resultJson": result_json,
        "captureTime": datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
    }
    ok = mqtt_publish(TOPIC_CAPTURE, json.dumps(payload, ensure_ascii=False))
    if not ok:
        print(f"[DB] MQTT 不可用, 直写 MySQL: {image_url}")
        db_insert_capture(image_url, person_count, result_json)


@app.route("/api/upload/photo", methods=["POST"])
def upload_photo():
    global latest_photo_path
    data = request.get_data()
    if not data:
        return "No data", 400
    filename = f"photo_{int(time.time() * 1000)}.jpg"
    filepath = os.path.join(PHOTO_DIR, filename)
    with open(filepath, "wb") as f:
        f.write(data)
    latest_photo_path = filepath
    print(f"[拍照] ✅ {filename} ({len(data)} 字节)")

    # 读取缓存的 AI 检测结果 (不清空, 让检测线程覆盖)
    global pending_ai_result
    with pending_ai_lock:
        ai = dict(pending_ai_result) if pending_ai_result else {}
    report_capture(
        image_url=f"/api/photo/{filename}",
        person_count=ai.get("person_count", 0) if ai else 0,
        result_json=ai.get("result_json", None) if ai else None,
    )

    sse_broadcast("photo", filename)
    return jsonify({"ok": True, "filename": filename}), 200


@app.route("/api/photo/latest")
def latest_photo():
    if latest_photo_path and os.path.exists(latest_photo_path):
        return send_file(latest_photo_path, mimetype="image/jpeg")
    return jsonify({"photo": None}), 404


@app.route("/api/photos")
def list_photos():
    try:
        files = sorted(
            (f for f in os.listdir(PHOTO_DIR) if f.lower().endswith(".jpg")),
            reverse=True,
        )
        return jsonify({"photos": files})
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/api/photo/<filename>")
def get_photo(filename):
    filepath = os.path.join(PHOTO_DIR, filename)
    if os.path.exists(filepath):
        return send_file(filepath, mimetype="image/jpeg")
    return jsonify({"error": "not found"}), 404


@app.route("/api/trigger/consume")
def trigger_consume():
    global trigger_flag
    with trigger_lock:
        if trigger_flag:
            trigger_flag = False
            return jsonify({"trigger": True})
        return jsonify({"trigger": False})


@app.route("/api/trigger/set", methods=["POST"])
def trigger_set():
    global trigger_flag, trigger_time
    data = request.get_json(force=True)
    if data.get("trigger"):
        with trigger_lock:
            trigger_flag = True
            trigger_time = time.time()
        return jsonify({"ok": True})
    return jsonify({"ok": False})


@app.route("/api/upload", methods=["POST"])
def upload_legacy():
    return upload_photo()


@app.route("/infer", methods=["POST"])
def infer():
    """组委会要求: POST http://127.0.0.1:5000/infer  body: {"image":"data:image/jpeg;base64,..."}
       内部转发到独立 YOLO 进程 (http://127.0.0.1:5001/infer)"""
    data = request.get_json(force=True)
    if not data or "image" not in data:
        return jsonify({"error": "no image"}), 400
    try:
        resp = requests.post(YOLO_URL, json=data, timeout=30)
        return (resp.content, resp.status_code, dict(resp.headers))
    except requests.exceptions.ConnectionError:
        return jsonify({"error": "YOLO server not running"}), 503


# ===== YOLO 自动检测线程 (通过代理) =====

def _norm_box(det, size):
    """把 YOLO 返回的检测框换算成 0~1 归一化坐标。
    老版 YOLO 服务只回 label/confidence/class (没有坐标), 这时返回 None,
    前端会自动退回"服务端标注流"模式, 不会报错。"""
    if det.get("bbox_norm"):
        return [float(v) for v in det["bbox_norm"]]
    bbox = det.get("bbox")
    if bbox and len(bbox) == 4:
        w = float(size.get("width") or 640)
        h = float(size.get("height") or 360)
        if w > 0 and h > 0:
            x1, y1, x2, y2 = (float(v) for v in bbox)
            return [round(x1 / w, 5), round(y1 / h, 5),
                    round(x2 / w, 5), round(y2 / h, 5)]
    return None


def detection_loop():
    global trigger_flag, trigger_time, pending_ai_result
    global latest_detection, annotated_frame, annotated_ts, yolo_online
    while True:
        # 有人在看实时画面 -> 高频推理 (画面里的框才跟得上人走动);
        # 没人看 -> 2 秒一次, 只负责"有人经过就抓拍"
        live = viewer_active()
        time.sleep(DETECT_LIVE_INTERVAL if live else DETECT_IDLE_INTERVAL)

        # 超时重置: trigger_flag 置 True 后 15 秒无人拍照, 释放锁让后续检测继续
        if trigger_flag and time.time() - trigger_time > TRIGGER_TIMEOUT:
            with trigger_lock:
                if trigger_flag:
                    trigger_flag = False
                    print("[AI] ⏰ 触发超时, 重置检测锁")

        with stream_lock:
            frame = latest_stream
        if frame is None:
            continue
        try:
            b64 = base64.b64encode(frame).decode()
            resp = yolo_session.post(YOLO_URL,
                json={"image": f"data:image/jpeg;base64,{b64}",
                      # 没人看实时画面时不让 YOLO 再编码一张标注图, 省 CPU
                      "annotate": live,
                      "conf": PERSON_CONF},
                timeout=10)
            yolo_online = (resp.status_code == 200)
            if resp.status_code == 200:
                data = resp.json()
                size = data.get("image_size") or {}
                raw_dets = [d for d in data.get("inference_results", [])
                            if d.get("confidence", 0) >= PERSON_CONF]

                boxes = []
                for d in raw_dets:
                    box = {
                        "label": d.get("label", ""),
                        "confidence": round(float(d.get("confidence", 0)), 3),
                        "class": d.get("class"),
                        "is_person": d.get("class") == 0,
                    }
                    nb = _norm_box(d, size)
                    if nb:
                        box["bbox_norm"] = nb
                    boxes.append(box)

                persons = [d for d in raw_dets if d.get("class") == 0]

                with detection_lock:
                    latest_detection = {
                        "ts": time.time(),
                        "boxes": boxes,
                        "person_count": len(persons),
                        "width": size.get("width", 640),
                        "height": size.get("height", 360),
                        "latency_ms": data.get("latency_ms", 0),
                        # 前端据此判断能不能自己画框
                        "has_bbox": any("bbox_norm" in b for b in boxes) or not boxes,
                    }

                # 缓存 YOLO 服务端画好框的帧, 供 /api/stream/annotated 推送
                processed = data.get("processed_image")
                if processed:
                    try:
                        raw = processed.split(",", 1)[1] if "," in processed else processed
                        decoded = base64.b64decode(raw)
                        with annotated_lock:
                            annotated_frame = decoded
                            annotated_ts = time.time()
                    except Exception:
                        pass

                if persons and not trigger_flag:
                    with trigger_lock:
                        if not trigger_flag:
                            trigger_flag = True
                            trigger_time = time.time()
                            print(f"[AI] 🎯 检测到 {len(persons)} 人 → 自动触发拍照")
                            with pending_ai_lock:
                                pending_ai_result = {
                                    "person_count": len(persons),
                                    # 必须用 json.dumps: str(list) 产出的是单引号的
                                    # Python 字面量, 不是合法 JSON, 前端解析会失败
                                    "result_json": json.dumps([{
                                        "class": p.get("class"),
                                        "confidence": round(p.get("confidence", 0), 3),
                                        "bbox": p.get("bbox", [])
                                    } for p in persons], ensure_ascii=False),
                                }
        except Exception:
            yolo_online = False


def _placeholder_jpeg():
    buf = io.BytesIO()
    buf.write(bytes([0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46,
        0x49, 0x46, 0x00, 0x01, 0x01, 0x00, 0x00, 0x01, 0x00, 0x01,
        0x00, 0x00, 0xFF, 0xDB, 0x00, 0x43, 0x00, 0x08, 0x06, 0x06]))
    buf.seek(0)
    return buf


if __name__ == "__main__":
    threading.Thread(target=detection_loop, daemon=True).start()
    print("=" * 50)
    print("  智能灯杆 Flask 服务器 (AI 图像服务)")
    print("=" * 50)
    print(f"  图库: {PHOTO_DIR}")
    print(f"  http://0.0.0.0:5000")
    print(f"  YOLO 代理: {YOLO_URL}")
    print(f"  MQTT 上报: {TOPIC_CAPTURE} -> {MQTT_HOST}:{MQTT_PORT}")
    print(f"  自动检测: 有人看画面 {DETECT_LIVE_INTERVAL}s / 空闲 {DETECT_IDLE_INTERVAL}s")
    print(f"  实时监控: /api/stream/mjpeg  +  /api/detect/latest")
    print(f"  AI 标注流: /api/stream/annotated")
    print("=" * 50)
    # debug 模式的热重载会加载两次模块, 导致两个同 client-id 的 MQTT 客户端互踢
    # 部署/联调用 debug=False + use_reloader=False
    app.run(host="0.0.0.0", port=5000, debug=False, threaded=True, use_reloader=False)
