import logging
import os
import sys
from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.routers import face
from app.schemas import ApiResponse, HealthResponse

logging.basicConfig(
    level=logging.DEBUG if settings.debug else logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting Face Recognition Algorithm Service...")
    logger.info(f"Debug mode: {settings.debug}")
    logger.info(f"MinIO endpoint: {settings.minio_endpoint}")
    
    os.makedirs(settings.temp_dir, exist_ok=True)
    
    try:
        from app.core.engine import get_face_engine
        engine = get_face_engine()
        logger.info("Face detection and recognition models loaded successfully")
    except Exception as e:
        logger.error(f"Failed to load models: {e}")
        raise
    
    yield
    
    logger.info("Shutting down Face Recognition Algorithm Service...")


app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    description="Face Detection and Recognition API Service for Classroom Attendance System",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(face.router)


@app.get("/health")
async def health_check() -> HealthResponse:
    try:
        from app.core.engine import get_face_engine
        engine = get_face_engine()
        
        return HealthResponse(
            status="healthy",
            models_loaded=True,
            yolo_loaded=engine._yolo_model is not None,
            insightface_loaded=engine._face_analyzer is not None
        )
    except Exception:
        return HealthResponse(
            status="unhealthy",
            models_loaded=False,
            yolo_loaded=False,
            insightface_loaded=False
        )


@app.get("/")
async def root() -> ApiResponse:
    return ApiResponse(
        code=0,
        msg="Face Recognition Algorithm Service is running",
        data={
            "name": settings.app_name,
            "version": settings.app_version
        }
    )


if __name__ == "__main__":
    uvicorn.run(
        "app.main:app",
        host=settings.host,
        port=settings.port,
        reload=settings.debug
    )
