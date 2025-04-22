package com.ll.quizzle.domain.room.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ll.quizzle.domain.room.dto.request.RoomCreateRequest;
import com.ll.quizzle.domain.room.dto.request.RoomUpdateRequest;
import com.ll.quizzle.domain.room.dto.response.RoomResponse;
import com.ll.quizzle.domain.room.service.RoomService;
import com.ll.quizzle.global.request.Rq;
import com.ll.quizzle.global.response.RsData;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms")
@Tag(name = "방 관리", description = "방 생성, 조회, 입장, 퇴장 등의 API")
@Slf4j
public class RoomController {
    
    private final RoomService roomService;
    private final Rq rq;
    
    @PostMapping
    @Operation(summary = "방 생성", description = "새로운 게임 방을 생성합니다.")
    public RsData<RoomResponse> createRoom(
            @RequestBody RoomCreateRequest request
    ) {
        RoomResponse response = roomService.createRoom(rq.getActor().getId(), request);
        return RsData.success(HttpStatus.CREATED, response);
    }
    
    @GetMapping
    @Operation(summary = "활성화된 방 목록 조회", description = "현재 활성화된 모든 방의 목록을 조회합니다.")
    public RsData<List<RoomResponse>> activeRooms() {
        List<RoomResponse> responses = roomService.getActiveRooms();
        return RsData.success(HttpStatus.OK, responses);
    }

    @GetMapping("/{roomId}")
    @Operation(summary = "특정 방 정보 조회", description = "특정 방의 상세 정보를 조회합니다. 로비에서 부분 갱신을 위해 사용됩니다.")
    public RsData<RoomResponse> getRoom(@PathVariable Long roomId) {
        RoomResponse response = roomService.getRoom(roomId);
        
        return RsData.success(HttpStatus.OK, response);
    }

    @PostMapping("/{roomId}/join")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "방 입장", description = "특정 방에 입장합니다. 비공개 방인 경우 비밀번호가 필요합니다.")
    public void joinRoom(
            @PathVariable Long roomId,
            @RequestParam(required = false) String password
    ) {
        roomService.joinRoom(roomId, rq.getActor().getId(), password);
    }
    
    @PostMapping("/{roomId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "방 퇴장", description = "현재 입장해 있는 방에서 퇴장합니다. 방장이 퇴장하면 다른 플레이어가 있을 경우 방장 권한이 위임되고, 다른 플레이어가 없을 경우 방이 삭제됩니다.")
    public void leaveRoom(
            @PathVariable Long roomId
    ) {
        roomService.leaveRoom(roomId, rq.getActor().getId());
    }
    
    @PostMapping("/{roomId}/leave-with-id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "방 퇴장 (ID 파라미터)", description = "현재 입장해 있는 방에서 퇴장합니다. 특히 브라우저 종료나 새로고침 시 userId 파라미터로 사용자 ID를 받아 처리합니다.")
    public void leaveRoomWithParam(
            @PathVariable Long roomId,
            @RequestParam Long userId
    ) {
        roomService.leaveRoom(roomId, userId);
    }
    
    @PostMapping("/{roomId}/ready")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "준비 상태 토글", description = "게임 준비 상태를 토글합니다. 방장은 준비 상태가 적용되지 않습니다.")
    public void toggleReady(
            @PathVariable Long roomId
    ) {
        roomService.toggleReady(roomId, rq.getActor().getId());
    }
    
    @PostMapping("/{roomId}/start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "게임 시작", description = "게임을 시작합니다. 방장만 시작할 수 있으며, 모든 플레이어가 준비 상태여야 합니다.")
    public void startGame(
            @PathVariable Long roomId
    ) {
        roomService.startGame(roomId, rq.getActor().getId());
    }

    @PutMapping("/{roomId}")
    @Operation(summary = "방 정보 업데이트", description = "방 정보를 업데이트합니다. 방장만 수행할 수 있습니다.")
    public RsData<RoomResponse> updateRoom(
            @PathVariable Long roomId,
            @RequestBody RoomUpdateRequest request
    ) {
        RoomResponse response = roomService.updateRoom(roomId, rq.getActor().getId(), request);
        return RsData.success(HttpStatus.OK, response);
    }
}
