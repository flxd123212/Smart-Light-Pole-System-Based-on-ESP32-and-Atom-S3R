import os
import sys
import time

# 脚本所在目录 (模型/图片都用绝对路径, 兼容任何工作目录)
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

from flask import Flask, request, jsonify
from ultralytics import YOLO
import cv2
import base64
import numpy as np
from flask_cors import CORS
import torch

# 初始化 Flask 应用
app = Flask(__name__)
CORS(app)
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # 16MB

# 加载 YOLO 模型 (Linux 兼容版: 路径取脚本所在目录)
MODEL_PATH = os.path.join(SCRIPT_DIR, "yolo11n.pt")
model = YOLO(MODEL_PATH)

# 推理分辨率 (整帧拉伸, 前端用归一化坐标还原, 不受形变影响)
INFER_W = int(os.environ.get("YOLO_INFER_W", "640"))
INFER_H = int(os.environ.get("YOLO_INFER_H", "360"))
# 实时流每秒要推理好几帧, 默认关闭调试图片落盘 (否则 SSD 一直在写)
SAVE_DEBUG = os.environ.get("YOLO_SAVE_DEBUG", "0") == "1"

_MIME_PREFIXES = (
    'data:image/jpeg;base64,', 'data:image/png;base64,',
    'data:image/gif;base64,', 'data:image/bmp;base64,',
    'data:image/webp;base64,',
)


@app.route('/infer', methods=['POST'])
def infer():
    t0 = time.time()
    data = request.json
    if not data or 'image' not in data:
        return jsonify({'error': 'No image provided'}), 400

    base64_image = data['image']
    # annotate=False 时不回传画好框的图片 (前端自己用 canvas 画框, 省带宽/省编码)
    annotate = bool(data.get('annotate', True))
    try:
        conf_th = float(data.get('conf', 0.25))
    except (TypeError, ValueError):
        conf_th = 0.25

    # 去掉 MIME 前缀
    for prefix in _MIME_PREFIXES:
        if base64_image.startswith(prefix):
            base64_image = base64_image.split(prefix)[1]
            break

    # 解码 Base64 图片
    try:
        image_data = base64.b64decode(base64_image)
        image_array = np.frombuffer(image_data, np.uint8)
        image = cv2.imdecode(image_array, cv2.IMREAD_COLOR)
        if image is None or image.size == 0:
            return jsonify({'error': 'Failed to decode image: Image is empty'}), 400
        src_h, src_w = image.shape[:2]
        image = cv2.resize(image, (INFER_W, INFER_H))  # 640x360 for speed
    except Exception as e:
        return jsonify({'error': f'Failed to decode image: {str(e)}'}), 400

    if SAVE_DEBUG:
        cv2.imwrite(os.path.join(SCRIPT_DIR, 'received_image.jpg'), image)

    # YOLO 推理
    try:
        results = model(image, verbose=False)
    except Exception as e:
        return jsonify({'error': f'Inference failed: {str(e)}'}), 500

    detections = []
    person_count = 0
    if hasattr(results[0], 'boxes'):
        for i, box in enumerate(results[0].boxes.xyxy):
            conf = float(results[0].boxes.conf[i])
            if conf < conf_th:
                continue
            x1, y1, x2, y2 = (int(v) for v in box)
            cls = int(results[0].boxes.cls[i])
            label = f"{model.names[cls]}"
            if cls == 0:
                person_count += 1
            detections.append({
                'label': label,
                'confidence': conf,
                'class': cls,
                # 推理图坐标 (INFER_W x INFER_H)
                'bbox': [x1, y1, x2, y2],
                # 归一化坐标 0~1: 前端按显示尺寸缩放即可, 与分辨率/形变无关
                'bbox_norm': [
                    round(x1 / INFER_W, 5), round(y1 / INFER_H, 5),
                    round(x2 / INFER_W, 5), round(y2 / INFER_H, 5),
                ],
            })

    response = {
        'inference_results': detections,
        'person_count': person_count,
        'image_size': {'width': INFER_W, 'height': INFER_H,
                       'source_width': src_w, 'source_height': src_h},
        'latency_ms': int((time.time() - t0) * 1000),
    }

    if annotate:
        # 保存/回传推理后的图像 (服务端画框版本)
        try:
            plotted = results[0].plot()
            if SAVE_DEBUG:
                cv2.imwrite(os.path.join(SCRIPT_DIR, 'inferred_image.jpg'), plotted)
            ok, buffer = cv2.imencode('.jpg', plotted,
                                      [int(cv2.IMWRITE_JPEG_QUALITY), 80])
            if not ok:
                raise RuntimeError('imencode failed')
            processed_image_data = base64.b64encode(buffer).decode('utf-8')
        except Exception as e:
            return jsonify({'error': f'Failed to encode image: {str(e)}'}), 500
        response['processed_image'] = "data:image/jpeg;base64," + processed_image_data

    return jsonify(response)


@app.route('/health')
def health():
    return jsonify({'ok': True, 'model': os.path.basename(MODEL_PATH),
                    'infer_size': [INFER_W, INFER_H]})


if __name__ == '__main__':
    print("=" * 50)
    print("  YOLO11 推理服务 (Linux 兼容版)")
    print(f"  模型: {MODEL_PATH}")
    print(f"  推理尺寸: {INFER_W}x{INFER_H}  调试落盘: {SAVE_DEBUG}")
    print(f"  http://0.0.0.0:5001/infer")
    print("=" * 50)
    app.run(host='0.0.0.0', port=5001, threaded=True)
