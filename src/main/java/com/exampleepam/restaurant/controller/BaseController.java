package com.exampleepam.restaurant.controller;

public class BaseController {

    //    Params
    protected static final String SORT_FIELD_PARAM = "sortField";
    protected static final String SORT_DIR_PARAM = "sortDir";
    protected static final String PAGE_SIZE_PARAM = "pageSize";
    protected static final String PAGE_NUMBER_PARAM = "pageNo";
    protected static final String CURRENT_PAGE_PARAM = "currentPage";
    protected static final String FILTER_CATEGORY_PARAM = "filterCategory";
    protected static final String REVERSE_SORT_DIR_PARAM = "reverseSortDir";
    protected static final String STATUS_PARAM = "status";

    // Attributes
    protected static final String DISH_LIST_ATTRIBUTE = "dishList";
    protected static final String ORDER_LIST_ATTRIBUTE = "orderList";
    public static final String USER_BALANCE_SESSION_ATTRIBUTE = "userBalance";
    public static final String USER_CART_ITEMS_SESSION_ATTRIBUTE = "userCartItemsTotalNumber";
    protected static final String ORDER_CREATION_TIME_FIELD = "creationDateTime";

    // Redirects
    protected static final String REDIRECT_TO_MENU = "redirect:/menu";

    // Pages
    protected static final String MENU_PAGE = "menu";

    // General
    protected static final String ASCENDING_ORDER_SORTING = "asc";
    protected static final String DESCENDING_ORDER_SORTING = "desc";
}
