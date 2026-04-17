package com.project.backend.mapper;

import com.project.backend.pojo.dto.WarningQueryDTO;
import com.project.backend.pojo.vo.WarningNoticeVO;
import com.project.backend.pojo.vo.WarningRankingVO;
import com.project.backend.pojo.vo.WarningTimelineVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 预警中心查询 Mapper 接口。
 */
@Mapper
public interface WarningMapper {

    /**
     * 查询缺勤排行列表。
     */
    List<WarningRankingVO> listWarningRankings(@Param("teacherId") Long teacherId,
                                              @Param("query") WarningQueryDTO queryDTO);

    /**
     * 查询单个学生在指定课程下的缺勤排行详情。
     */
    WarningRankingVO getWarningRankingDetail(@Param("teacherId") Long teacherId,
                                            @Param("courseId") Long courseId,
                                            @Param("studentId") Long studentId,
                                            @Param("query") WarningQueryDTO queryDTO);

    /**
     * 查询学生考勤轨迹。
     */
    List<WarningTimelineVO> listWarningTimelines(@Param("teacherId") Long teacherId,
                                                 @Param("courseId") Long courseId,
                                                 @Param("studentId") Long studentId,
                                                 @Param("query") WarningQueryDTO queryDTO);

    /**
     * 查询通知记录列表。
     */
    List<WarningNoticeVO> listNoticeHistory(@Param("teacherId") Long teacherId,
                                            @Param("query") WarningQueryDTO queryDTO);
}
