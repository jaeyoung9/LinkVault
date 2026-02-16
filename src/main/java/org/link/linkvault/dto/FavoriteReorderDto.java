package org.link.linkvault.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FavoriteReorderDto {

    private List<Item> items;

    @Getter
    @Setter
    public static class Item {
        private Long bookmarkId;
        private int displayOrder;
    }
}
