package com.example.library.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.library.dto.StudyRoomAddRequest;
import com.example.library.dto.StudyRoomUpdateRequest;
import com.example.library.entity.Seat;
import com.example.library.entity.StudyRoom;
import com.example.library.exception.BusinessException;
import com.example.library.mapper.SeatMapper;
import com.example.library.mapper.StudyRoomMapper;
import com.example.library.service.StudyRoomService;
import com.example.library.vo.StudyRoomVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import static com.example.library.config.RedisConfig.CACHE_STUDY_ROOM_ENABLED;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 自习室业务实现
 */
@Service
@RequiredArgsConstructor
public class StudyRoomServiceImpl extends ServiceImpl<StudyRoomMapper, StudyRoom> implements StudyRoomService {

    private final SeatMapper seatMapper;

    @Override
    @CacheEvict(cacheNames = CACHE_STUDY_ROOM_ENABLED, allEntries = true)
    public void addStudyRoom(StudyRoomAddRequest request) {
        StudyRoom room = new StudyRoom();
        room.setName(request.getName());
        room.setLocation(request.getLocation());
        room.setCapacity(request.getCapacity());
        room.setOpenTime(request.getOpenTime());
        room.setStatus(request.getStatus() == null ? 1 : request.getStatus());

        if (!save(room)) {
            throw new BusinessException("新增自习室失败，请稍后重试");
        }
    }

    @Override
    @CacheEvict(cacheNames = CACHE_STUDY_ROOM_ENABLED, allEntries = true)
    public void updateStudyRoom(StudyRoomUpdateRequest request) {
        StudyRoom room = getById(request.getId());
        if (room == null) {
            throw new BusinessException("自习室不存在");
        }

        room.setName(request.getName());
        room.setLocation(request.getLocation());
        room.setCapacity(request.getCapacity());
        room.setOpenTime(request.getOpenTime());
        if (request.getStatus() != null) {
            room.setStatus(request.getStatus());
        }

        if (!updateById(room)) {
            throw new BusinessException("修改自习室失败，请稍后重试");
        }
    }

    @Override
    @CacheEvict(cacheNames = CACHE_STUDY_ROOM_ENABLED, allEntries = true)
    public void deleteStudyRoom(Long id) {
        if (id == null) {
            throw new BusinessException("自习室ID不能为空");
        }
        // 删除前检查是否存在座位
        Long seatCount = seatMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Seat>()
                        .eq("study_room_id", id)
        );
        if (seatCount != null && seatCount > 0) {
            throw new BusinessException("自习室下存在座位，无法删除");
        }
        if (!removeById(id)) {
            throw new BusinessException("自习室不存在或已被删除");
        }
    }

    @Override
    public List<StudyRoomVO> listAll() {
        List<StudyRoom> list = lambdaQuery()
                .orderByAsc(StudyRoom::getId)
                .list();
        return toVOList(list);
    }

    @Override
    @Cacheable(cacheNames = CACHE_STUDY_ROOM_ENABLED, key = "'list'")
    public List<StudyRoomVO> listEnabled() {
        List<StudyRoom> list = lambdaQuery()
                .eq(StudyRoom::getStatus, 1)
                .orderByAsc(StudyRoom::getId)
                .list();
        return toVOList(list);
    }

    @Override
    public StudyRoomVO getEnabledDetail(Long id) {
        StudyRoom room = lambdaQuery()
                .eq(StudyRoom::getId, id)
                .eq(StudyRoom::getStatus, 1)
                .one();
        if (room == null) {
            throw new BusinessException(404, "自习室不存在或已被禁用");
        }
        StudyRoomVO vo = new StudyRoomVO();
        BeanUtils.copyProperties(room, vo);
        return vo;
    }

    private List<StudyRoomVO> toVOList(List<StudyRoom> list) {
        return list.stream()
                .map(room -> {
                    StudyRoomVO vo = new StudyRoomVO();
                    BeanUtils.copyProperties(room, vo);
                    return vo;
                })
                .collect(Collectors.toList());
    }
}

