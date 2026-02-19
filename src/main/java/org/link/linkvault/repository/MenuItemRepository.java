package org.link.linkvault.repository;

import org.link.linkvault.entity.MenuItem;
import org.link.linkvault.entity.MenuType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    @Query("SELECT m FROM MenuItem m WHERE m.menuType = :menuType AND m.parent IS NULL ORDER BY m.displayOrder")
    List<MenuItem> findRootsByMenuType(@Param("menuType") MenuType menuType);

    @Query("SELECT m FROM MenuItem m WHERE m.menuType = :menuType AND m.parent IS NULL AND m.visible = true ORDER BY m.displayOrder")
    List<MenuItem> findVisibleRootsByMenuType(@Param("menuType") MenuType menuType);

    boolean existsByMenuTypeAndUrl(MenuType menuType, String url);
}
