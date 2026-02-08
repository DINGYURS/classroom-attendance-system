"""
测试脚本：使用 Ultralytics YOLO11 官方模型进行目标检测
注意：YOLO11 官方模型是在 COCO 数据集上训练的，类别 0 是 "person"（人物），
不是专门的人脸检测模型。如需专业人脸检测，建议后续使用 face-specific 数据集重新训练。
"""
import cv2
from ultralytics import YOLO


def detect_and_draw_objects(
    image_path: str,
    output_path: str = None,
    conf_threshold: float = 0.5,
    classes: list = None
):
    """
    使用 YOLO11 检测目标并标注

    Args:
        image_path: 输入图片路径
        output_path: 输出图片路径
        conf_threshold: 检测置信度阈值
        classes: 要检测的类别列表（None 表示检测所有类别）
                 COCO 类别：0=person, 1=bicycle, 2=car, ... (共 80 类)
    """
    # 加载用户训练的专用人脸识别模型
    model = YOLO("./weights/best-1280.pt")

    model.overrides['imgsz'] = 1280
    model.overrides['conf'] = 0.25

    # 读取图片
    image = cv2.imread(image_path)
    if image is None:
        print(f"❌ 无法读取图片: {image_path}")
        return

    # 进行推理（开启 TTA 增强）
    results = model(image, conf=conf_threshold, augment=True, verbose=False)

    # 统计人脸数量（模型只识别人脸，无需类别判断）
    face_count = 0
    for result in results:
        boxes = result.boxes
        if boxes is not None:
            for box in boxes:
                face_count += 1
                # 获取边界框坐标
                x1, y1, x2, y2 = map(int, box.xyxy[0])
                confidence = float(box.conf[0])

                # 检查置信度是否高于阈值
                if confidence < conf_threshold:
                    continue

                # 绘制边界框（绿色）
                cv2.rectangle(image, (x1, y1), (x2, y2), (0, 255, 0), 2)

                # 添加置信度标签
                label = f"Face: {confidence:.2f}"
                cv2.putText(image, label, (x1, y1 - 10),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)

    # 保存结果
    if output_path is None:
        output_path = image_path
    cv2.imwrite(output_path, image)

    print(f"✅ 检测完成！")
    print(f"   输入: {image_path}")
    print(f"   输出: {output_path}")
    print(f"   检测到目标数量: {face_count}")

    return face_count


def detect_persons(image_path: str, output_path: str = None, conf_threshold: float = 0.5):
    """
    检测图片中的人物（COCO 类别 0 = person）
    这是 YOLO11 官方模型最接近"人脸/人体"检测的功能
    """
    return detect_and_draw_objects(
        image_path,
        output_path,
        conf_threshold,
        classes=[0]  # COCO 类别 0 = person
    )


if __name__ == "__main__":
    # 配置路径
    IMAGE_PATH = "test5.jpg"      # 输入图片路径
    OUTPUT_PATH = "test5_output.jpg"    # 输出图片路径

    # 检测人物（COCO 类别 0 = person）
    # 注意：这是 YOLO11 官方模型能检测的最接近"人"的类别
    print("=" * 50)
    print("YOLO11 人物检测（检测完整人体，非人脸）")
    print("=" * 50)
    detect_persons(IMAGE_PATH, OUTPUT_PATH, conf_threshold=0.5)

    print("\n" + "=" * 50)
    print("说明：")
    print("- YOLO11 官方模型在 COCO 数据集上训练，包含 80 个类别")
    print("- 类别 0 = person（人物），检测的是完整人体")
    print("- 如需专业人脸检测，建议后续训练 face-specific 模型")
    print("=" * 50)
