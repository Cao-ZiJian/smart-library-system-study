package com.example.library.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.library.dto.StudyRoomAddRequest;
import com.example.library.dto.StudyRoomUpdateRequest;
import com.example.library.entity.StudyRoom;
import com.example.library.vo.StudyRoomVO;

import java.util.List;

/**
 * 自习室业务接口
 */
public interface StudyRoomService extends IService<StudyRoom> {

    /**
     * 新增自习室
     */
    void addStudyRoom(StudyRoomAddRequest request);

    /**
     * 修改自习室
     */
    void updateStudyRoom(StudyRoomUpdateRequest request);

    /**
     * 删除自习室
     */
    void deleteStudyRoom(Long id);

    /**
     * 自习室列表（管理端）
     */
    List<StudyRoomVO> listAll();

    /**
     * 自习室列表（用户端，仅启用）
     */
    List<StudyRoomVO> listEnabled();

    /**
     * 自习室详情（用户端，仅启用）
     */
    StudyRoomVO getEnabledDetail(Long id);
}

