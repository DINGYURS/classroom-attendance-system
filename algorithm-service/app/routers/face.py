import asyncio
import json
import logging
from concurrent.futures import ThreadPoolExecutor
from typing import List

from fastapi import APIRouter

from app.core.downloader import get_image_downloader
from app.core.engine import get_face_engine
from app.schemas import (
    ApiResponse,
    ExtractRequest,
    DetectRequest,
    DetectResponse,
    FaceInfo,
)

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/face", tags=["Face Recognition"])

# 专用线程池，避免 CPU 密集推理阻塞 asyncio 事件循环
_executor = ThreadPoolExecutor(max_workers=2, thread_name_prefix="face_infer")


@router.post("/extract")
async def extract_face_feature(request: ExtractRequest) -> ApiResponse:
    """
    单人人脸注册：下载图片 → 提取唯一一张人脸的特征向量（512维float列表）
    返回: { "code": 1, "data": "[0.123, ...]" }  —— data 是 JSON 序列化的 float 数组字符串
    Java 拿到 data 后：AES 加密 → 存入数据库
    """
    try:
        downloader = get_image_downloader()

        # 图片下载（含 HTTP I/O）也放到线程池，避免 httpx 同步客户端阻塞事件循环
        loop = asyncio.get_event_loop()
        image = await loop.run_in_executor(
            _executor, downloader.download_image, request.imageUrl
        )

        if image is None:
            return ApiResponse(code=0, msg="Failed to download image", data=None)

        engine = get_face_engine()

        # CPU 密集推理放到线程池执行
        feature = await loop.run_in_executor(
            _executor, engine.extract_single_face_feature, image
        )

        if feature is None:
            return ApiResponse(code=0, msg="No face detected in the image", data=None)

        # 返回 JSON 字符串，Java 直接 AES 加密后存库
        feature_json = json.dumps(feature)
        return ApiResponse(code=1, msg="success", data=feature_json)

    except Exception as e:
        logger.error(f"Extract face feature failed: {e}", exc_info=True)
        return ApiResponse(code=0, msg=str(e), data=None)


def _detect_single(downloader, engine, idx: int, image_url: str) -> DetectResponse:
    """同步函数：下载 + 推理单张图片，在线程池中执行"""
    logger.info("[FACE-DETECT-201] start_image index=%d url=%s", idx, image_url)
    image = downloader.download_image(image_url)
    if image is None:
        logger.warning("[FACE-DETECT-202] download_failed index=%d url=%s", idx, image_url)
        return DetectResponse(imageIndex=idx, faces=[])

    logger.info("[FACE-DETECT-203] download_ok index=%d image_shape=%s", idx, tuple(image.shape))
    faces_data = engine.detect_and_extract_all(image)
    faces = [
        FaceInfo(
            bbox=face["bbox"],
            embedding=face.get("embedding"),
            detScore=face.get("det_score", 0.0),
        )
        for face in faces_data
    ]
    embedding_ready_count = sum(1 for face in faces if face.embedding is not None)
    logger.info(
        "[FACE-DETECT-204] image_done index=%d faces=%d embedding_ready=%d embedding_missing=%d",
        idx,
        len(faces),
        embedding_ready_count,
        len(faces) - embedding_ready_count,
    )
    return DetectResponse(imageIndex=idx, faces=faces)


@router.post("/detect")
async def detect_faces(request: DetectRequest) -> ApiResponse:
    """
    考勤合照检测：从多张图片中检测并提取所有人脸的特征向量
    Java 拿到结果后，在 Java 内存中与数据库学生特征做余弦相似度比对
    返回: { "code": 1, "data": [ { "imageIndex": 0, "faces": [ { "bbox": [...], "embedding": [...], "detScore": 0.9 } ] } ] }
    """
    try:
        downloader = get_image_downloader()
        engine = get_face_engine()
        loop = asyncio.get_event_loop()

        logger.info("[FACE-DETECT-205] request_start image_count=%d", len(request.imageUrls))

        # 每张图片串行处理（insightface 非线程安全，不并发）
        results: List[DetectResponse] = []
        for idx, image_url in enumerate(request.imageUrls):
            result = await loop.run_in_executor(
                _executor, _detect_single, downloader, engine, idx, image_url
            )
            results.append(result)

        total_faces = sum(len(result.faces) for result in results)
        total_embeddings = sum(
            1 for result in results for face in result.faces if face.embedding is not None
        )
        logger.info(
            "[FACE-DETECT-206] request_done image_count=%d total_faces=%d total_embeddings=%d",
            len(results),
            total_faces,
            total_embeddings,
        )

        return ApiResponse(code=1, msg="success", data=[r.model_dump() for r in results])

    except Exception as e:
        logger.error(f"Detect faces failed: {e}", exc_info=True)
        return ApiResponse(code=0, msg=str(e), data=None)