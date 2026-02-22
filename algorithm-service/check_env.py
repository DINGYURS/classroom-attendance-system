import sys
print("Python:", sys.version)

try:
    import numpy as np
    print("numpy:", np.__version__, "OK")
except Exception as e:
    print("numpy FAILED:", e)

try:
    import cv2
    print("cv2:", cv2.__version__, "OK")
except Exception as e:
    print("cv2 FAILED:", e)

try:
    import onnxruntime as ort
    print("onnxruntime:", ort.__version__, "OK")
except Exception as e:
    print("onnxruntime FAILED:", e)

try:
    import insightface
    print("insightface:", insightface.__version__, "OK")
except Exception as e:
    print("insightface FAILED:", e)

try:
    from insightface.app import FaceAnalysis
    fa = FaceAnalysis(name="buffalo_l", providers=["CPUExecutionProvider"])
    fa.prepare(ctx_id=0, det_size=(640, 640))
    print("FaceAnalysis loaded OK")
except Exception as e:
    print("FaceAnalysis FAILED:", e)

print("All checks done.")

