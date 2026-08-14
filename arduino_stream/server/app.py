"""
智能灯杆 — Flask 服务器
======================
角色: 接收图片, 提供实时流+拍照触发+图库+YOLO代理
"""

import io, os, time, queue, threading, base64, datetime
import requests
import pymysql
from flask import (
    Flask, Response, jsonify, render_template, request, send_file, stream_with_context,
)

# ---- MySQL (共用 iot-light 库, 写摄像头抓拍记录) ----
# 单机测试: MySQL 在同一机器
DB_CONFIG = {
    "host": "localhost",
    "port": 3306,
    "user": "root",
    "password": "123456",
    "database": "iot-light",
    "charset": "utf8mb4",
}
# 双服务器部署: MySQL 在业务服务器 (取消注释, 注释上面单机部分)
# DB_CONFIG = {
#     "host": "192.168.223.134",
#     "port": 3306,
#     "user": "root",
#     "password": "123456",
#     "database": "iot-light",
#     "charset": "utf8mb4",
# }
POLE_ID = 100    # 对应 iot_pole.pole_id = LP001 (pole_code = 'LP001')
PHOTO_BASE_URL = "/api/photo/"  # 前端访问路径

def db_insert_capture(image_url, person_count, result_json=None):
    """写入 iot_camera_capture 表"""
    try:
        conn = pymysql.connect(**DB_CONFIG)
        with conn.cursor() as cur:
            sql = """INSERT INTO iot_camera_capture
                     (pole_id, image_url, person_count, result_json, capture_time,
                      del_flag, create_by, create_time)
                     VALUES (%s,%s,%s,%s,NOW(),'0','system',NOW())"""
            cur.execute(sql, (POLE_ID, image_url, person_count, result_json))
            conn.commit()
        conn.close()
        return True
    except Exception as e:
        print(f"[DB] 写入失败: {e}")
        return False

app = Flask(__name__)
app.config['JSONIFY_PRETTYPRINT_REGULAR'] = False

YOLO_URL = "http://127.0.0.1:5001/infer"

PHOTO_DIR = os.path.join(os.path.dirname(__file__), "photos")
os.makedirs(PHOTO_DIR, exist_ok=True)

trigger_flag = False
trigger_lock = threading.Lock()

latest_stream = None
stream_lock = threading.Lock()
latest_photo_path = None

camera_ip = None
camera_ip_lock = threading.Lock()

sse_clients = []
sse_clients_lock = threading.Lock()

pending_ai_result = {}   # {"person_count": N, "result_json": "{...}"}
pending_ai_lock = threading.Lock()


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
        try:
            while True:
                event, data = q.get()
                yield f"event: {event}\ndata: {data}\n\n"
        except GeneratorExit:
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
    global latest_stream, camera_ip
    data = request.get_data()
    if not data:
        return "No data", 400
    with stream_lock:
        latest_stream = bytes(data)
    client_ip = request.remote_addr
    if client_ip and client_ip != camera_ip:
        with camera_ip_lock:
            camera_ip = client_ip
        print(f"[相机] ✅ 自动注册 IP: {camera_ip}")
    return jsonify({"ok": True}), 200


@app.route("/api/stream/latest")
def stream_latest():
    with stream_lock:
        if latest_stream:
            return Response(latest_stream, mimetype="image/jpeg")
    return send_file(_placeholder_jpeg(), mimetype="image/jpeg")


@app.route("/api/stream/mjpeg")
def stream_mjpeg():
    def generate():
        last_frame = None
        idle = 0
        while True:
            with stream_lock:
                frame = latest_stream
            if frame is not None and frame is not last_frame:
                last_frame = frame
                idle = 0
                yield b"--frame\r\nContent-Type: image/jpeg\r\n\r\n" + frame + b"\r\n"
                time.sleep(0.05)
            else:
                idle += 1
                if idle > 400:
                    break
                time.sleep(0.025)
    return Response(generate(), mimetype="multipart/x-mixed-replace; boundary=frame")


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

    # 写入 RuoYi 数据库 iot_camera_capture 表
    global pending_ai_result
    with pending_ai_lock:
        ai = pending_ai_result
        pending_ai_result = {}
    db_insert_capture(
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
    global trigger_flag
    data = request.get_json(force=True)
    if data.get("trigger"):
        with trigger_lock:
            trigger_flag = True
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
        return jsonify({"error": "YOLO server not running (run D:\\yolo_model\\run.bat)"}), 503


# ===== YOLO 自动检测线程 (通过代理) =====

def detection_loop():
    global trigger_flag, trigger_lock, pending_ai_result
    while True:
        time.sleep(2.0)
        with stream_lock:
            frame = latest_stream
        if frame is None:
            continue
        try:
            b64 = base64.b64encode(frame).decode()
            resp = requests.post(YOLO_URL,
                json={"image": f"data:image/jpeg;base64,{b64}"},
                timeout=10)
            if resp.status_code == 200:
                data = resp.json()
                persons = [d for d in data.get("inference_results", [])
                           if d.get("class") == 0 and d.get("confidence", 0) > 0.5]
                if persons and not trigger_flag:
                    with trigger_lock:
                        if not trigger_flag:
                            trigger_flag = True
                            print(f"[AI] 🎯 检测到 {len(persons)} 人 → 自动触发拍照")
                            # 缓存 AI 结果, upload_photo 会用
                            with pending_ai_lock:
                                pending_ai_result = {
                                    "person_count": len(persons),
                                    "result_json": str([{
                                        "class": p.get("class"),
                                        "confidence": round(p.get("confidence", 0), 3),
                                        "bbox": p.get("bbox", [])
                                    } for p in persons]),
                                }
        except Exception:
            pass


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
    print("  智能灯杆 Flask 服务器")
    print("=" * 50)
    print(f"  📁 图库: {PHOTO_DIR}")
    print(f"  🌐 http://0.0.0.0:5000")
    print(f"  🔬 YOLO 代理: {YOLO_URL}")
    print("  ⚡ 自动检测: 每 2 秒")
    print("=" * 50)
    app.run(host="0.0.0.0", port=5000, debug=True, threaded=True)