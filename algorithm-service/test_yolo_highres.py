import cv2
from ultralytics import YOLO
import numpy as np


def detect_high_res(
    image_path: str,
    model_path: str = "./weights/best-1280.pt",
    conf_threshold: float = 0.25,
    imgsz: int = 1280
):
    model = YOLO(model_path)

    image = cv2.imread(image_path)
    if image is None:
        print(f"Cannot read image: {image_path}")
        return

    h, w = image.shape[:2]
    print(f"Image size: {w}x{h}")

    results = model(image, conf=conf_threshold, imgsz=imgsz, augment=True, verbose=False)

    all_boxes = []
    for result in results:
        if result.boxes is not None:
            for box in result.boxes:
                all_boxes.append({
                    'xyxy': box.xyxy[0].cpu().numpy(),
                    'conf': float(box.conf[0])
                })

    for box in all_boxes:
        x1, y1, x2, y2 = map(int, box['xyxy'])
        cv2.rectangle(image, (x1, y1), (x2, y2), (0, 255, 0), 3)
        cv2.putText(image, f"Face: {box['conf']:.2f}", (x1, y1 - 10),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 0), 2)

    output_path = image_path.replace('.jpg', '_output.jpg')
    cv2.imwrite(output_path, image)

    print(f"Detected faces: {len(all_boxes)}")
    print(f"Output saved: {output_path}")


if __name__ == "__main__":
    detect_high_res("test5.jpg", conf_threshold=0.25, imgsz=1280)
