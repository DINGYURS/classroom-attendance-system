from sahi import AutoDetectionModel
from sahi.predict import get_sliced_prediction
import logging
import os
from PIL import Image  # 用于将 PNG 转换为 JPG
#TODO: 该文件代码依然存在错误，推理图片会出现极小区域误检人脸的问题，不知道是数据集问题，还是训练代码问题，还是迁移学习时候没有选对模型

# 过滤 ultralytics 的冗余日志
logging.getLogger("ultralytics").setLevel(logging.ERROR)

def detect_with_sahi(
        image_path: str,
        model_path: str = "./weights/best-1280.pt", # 请确认你的模型路径正确
        conf_threshold: float = 0.25,
        slice_height: int = 1280,
        slice_width: int = 1280,
        overlap_height_ratio: float = 0.2,
        overlap_width_ratio: float = 0.2
):
    print(f"正在加载模型: {model_path} ...")

    # 1. 加载模型 (修复了之前的报错)
    detection_model = AutoDetectionModel.from_pretrained(
        model_type="yolov8",
        model_path=model_path,
        confidence_threshold=conf_threshold,
        device="cuda:0"  # 使用 GPU
    )

    print("模型加载完成，开始切片推理...")

    # 2. 执行推理
    result = get_sliced_prediction(
        image_path,
        detection_model,
        slice_height=slice_height,
        slice_width=slice_width,
        overlap_height_ratio=overlap_height_ratio,
        overlap_width_ratio=overlap_width_ratio,
        verbose=1
    )

    return result

if __name__ == "__main__":
    image_file = "test9.jpg"       # 输入图片
    output_name = "test9_output"   # 输出文件名 (不带后缀)

    try:
        # 1. 推理
        result = detect_with_sahi(
            image_path=image_file,
            conf_threshold=0.3,
            slice_height=1280,
            slice_width=1280
        )
        print(f"\n>>> 成功检测到 {len(result.object_prediction_list)} 张人脸")

        # 2. 让 SAHI 生成临时的 PNG (无法跳过这一步)
        # 这会在目录下生成 test5_output.png
        result.export_visuals(
            export_dir=".",
            file_name=output_name,

            # 👇 新增这两个参数来控制样式
            text_size=0.5,  # 字体大小 (默认值通常很大，建议改小，比如 0.3 或 0.5)
            rect_th=2,      # 边框粗细 (默认根据图片大小自动计算，建议手动设为 1-3)
            # text_th=1       # 字体线条粗细 (如果字看起来太粗糊在一起，设为 1)
        )

        # 定义文件名
        png_path = f"{output_name}.png"
        jpg_path = f"{output_name}.jpg"

        # 3. 转换格式并清理
        if os.path.exists(png_path):
            # 打开 PNG 转为 JPG
            img = Image.open(png_path).convert("RGB")
            img.save(jpg_path, quality=95)
            print(f">>> 结果已保存为: {jpg_path}")

            # 【关键一步】删除那个讨厌的 PNG 文件
            os.remove(png_path)
            print(">>> 临时 PNG 文件已自动删除")

        else:
            print("警告: 未找到中间 PNG 文件")

    except Exception as e:
        print(f"\n!!! 发生错误: {e}")