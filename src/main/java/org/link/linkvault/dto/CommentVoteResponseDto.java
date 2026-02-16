package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.VoteType;

@Getter
@Builder
public class CommentVoteResponseDto {

    private int likeCount;
    private int dislikeCount;
    private int score;
    private VoteType userVote;
}
