package com.hrsystem.delivery.cli.views;

import com.hrsystem.domain.enums.UserRole;

/**
 * Полиморфное меню роли: CliRunner не знает о конкретных экранах, а просто
 * выбирает подходящую реализацию по роли текущей сессии.
 */
public interface RoleMenu {

    boolean supports(UserRole role);

    void open();
}
