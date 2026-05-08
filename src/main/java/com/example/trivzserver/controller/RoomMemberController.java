package com.example.trivzserver.controller;

import com.example.trivzserver.dto.RoomMemberResponse;
import com.example.trivzserver.entity.RoomMember;
import com.example.trivzserver.service.RoomMemberService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomId}")
public class RoomMemberController {

    private final RoomMemberService roomMemberService;

    public RoomMemberController(RoomMemberService roomMemberService) {
        this.roomMemberService = roomMemberService;
    }

    @GetMapping("/members")
    public List<RoomMemberResponse> listMembers(@PathVariable Long roomId) {
        return roomMemberService.listMembers(roomId).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/join")
    public RoomMemberResponse join(@PathVariable Long roomId) {
        RoomMember member = roomMemberService.join(roomId);
        return toResponse(member);
    }

    @PostMapping("/leave")
    public void leave(@PathVariable Long roomId) {
        roomMemberService.leave(roomId);
    }

    private RoomMemberResponse toResponse(RoomMember member) {
        return new RoomMemberResponse(
                member.getId(),
                member.getRoom().getId(),
                member.getPlayer().getId(),
                member.getPlayer().getUsername(),
                member.getJoinedAt()
        );
    }
}