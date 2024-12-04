package com.prgrms.ijuju.domain.chat.service;

import lombok.RequiredArgsConstructor;

import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.RedisTemplate;

import com.prgrms.ijuju.domain.chat.dto.request.ChatMessageRequestDTO;
import com.prgrms.ijuju.domain.chat.dto.response.ChatMessageResponseDTO;
import com.prgrms.ijuju.domain.chat.dto.response.ChatRoomListResponseDTO;
import com.prgrms.ijuju.domain.chat.dto.response.ChatReadResponseDTO;
import com.prgrms.ijuju.domain.chat.entity.Chat;
import com.prgrms.ijuju.domain.chat.entity.ChatRoom;
import com.prgrms.ijuju.domain.member.entity.Member;
import com.prgrms.ijuju.domain.chat.exception.ChatException;
import com.prgrms.ijuju.domain.chat.repository.ChatRepository;
import com.prgrms.ijuju.domain.chat.repository.ChatRoomRepository;
import com.prgrms.ijuju.domain.chat.repository.ChatMessageRepository;
import com.prgrms.ijuju.domain.member.repository.MemberRepository;
import com.prgrms.ijuju.domain.chat.exception.ChatTaskException;

import java.util.List;
import java.util.Comparator;
import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {
    
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRepository chatRepository;
    private final MemberRepository memberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // 채팅방 목록 조회
    public List<ChatRoomListResponseDTO> getChatRoomList(Long userId) {
        return chatRoomRepository.findByMemberIdOrFriendId(userId, userId).stream()
                .filter(room -> !room.isDeleted())
                .map(room -> ChatRoomListResponseDTO.from(room, userId))
                .sorted(Comparator.comparing(
                    dto -> dto.getLastMessageTime(), 
                    Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    // 채팅방 생성 또는 복구
    public ChatRoom createOrRestoreChatRoom(Long memberId, Long friendId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> ChatException.USER_NOT_FOUND.toException());
        Member friend = memberRepository.findById(friendId)
                .orElseThrow(() -> ChatException.USER_NOT_FOUND.toException());

        List<ChatRoom> existingRooms = chatRoomRepository
                .findByMemberIdOrFriendId(memberId, friendId);

        if (!existingRooms.isEmpty()) {
            ChatRoom room = existingRooms.get(0);
            if (room.isDeleted()) {
                room.restore();
            }
            return room;
        }

        return chatRoomRepository.save(ChatRoom.builder()
                .member(member)
                .friend(friend)
                .build());
    }

    // 채팅방 삭제
    public void deleteChatRoom(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> ChatException.CHATROOM_NOT_FOUND.toException());

        if (!room.getMember().getId().equals(userId) && 
            !room.getFriend().getId().equals(userId)) {
            throw ChatException.CHATROOM_ACCESS_DENIED.toException();
        }

        room.markAsDeleted();
    }

    // 채팅방 메시지 조회
    public List<ChatMessageResponseDTO> getMessagesByChatRoomId(Long roomId, Long userId) {
        return chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId)
                .stream()
                .map(ChatMessageResponseDTO::from)
                .collect(Collectors.toList());
    }

    // 메시지 전송
    public ChatMessageResponseDTO sendMessage(ChatMessageRequestDTO request, Long senderId) {
        ChatRoom room = chatRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> ChatException.CHATROOM_NOT_FOUND.toException());
        
        Member sender = memberRepository.findById(senderId)
            .orElseThrow(() -> ChatException.USER_NOT_FOUND.toException());

        Chat message = Chat.builder()
                .chatRoom(room)
                .sender(sender)
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .createdAt(LocalDateTime.now())
                .isRead(false)
                .isDeleted(false)
                .build();
                
        Chat savedMessage = chatMessageRepository.save(message);
        
        // Redis pub/sub으로 실시간 메시지 전송
        redisTemplate.convertAndSend("chat." + room.getId(), 
            ChatMessageResponseDTO.from(savedMessage));
            
        return ChatMessageResponseDTO.from(savedMessage);
    }

    // 메시지 읽음 처리
    public ChatReadResponseDTO markAsRead(Long roomId, Long userId) {
        List<Chat> unreadMessages = chatMessageRepository
            .findByChatRoomIdAndCreatedAtGreaterThanEqual(roomId, LocalDateTime.now().minusDays(14));
            
        unreadMessages.forEach(message -> {
            message.setRead(true);
            chatMessageRepository.save(message);
        });
        
        return ChatReadResponseDTO.builder()
            .roomId(roomId)
            .userId(userId)
            .readAt(LocalDateTime.now())
            .build();
    }

    // 메시지 삭제
    public void deleteMessage(Long messageId, Long userId) {
        Chat chat = chatRepository.findById(messageId)
                .orElseThrow(() -> ChatException.MESSAGE_NOT_FOUND.toException());

        if (!chat.getSender().getId().equals(userId)) {
            throw ChatException.MESSAGE_DELETION_DENIED.toException();
        }

        chat.delete();
    }
}
