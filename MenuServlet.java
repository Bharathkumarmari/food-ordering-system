package com.foodapp.controller;

import com.foodapp.dao.MenuDAO;
import com.foodapp.dao.RestaurantDAO;
import com.foodapp.model.MenuItem;
import com.foodapp.model.Restaurant;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * MenuServlet — Handles menu display, search, and category filtering.
 */
public class MenuServlet extends HttpServlet {

    private MenuDAO menuDAO;
    private RestaurantDAO restaurantDAO;

    @Override
    public void init() throws ServletException {
        menuDAO = new MenuDAO();
        restaurantDAO = new RestaurantDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        if (action == null) action = "list";

        switch (action) {
            case "search":
                searchMenu(request, response);
                break;
            case "filter":
                filterByCategory(request, response);
                break;
            default:
                listMenu(request, response);
                break;
        }
    }

    private void listMenu(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String restaurantIdStr = request.getParameter("restaurantId");

        if (restaurantIdStr == null || restaurantIdStr.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/restaurants");
            return;
        }

        try {
            int restaurantId = Integer.parseInt(restaurantIdStr);
            Restaurant restaurant = restaurantDAO.getRestaurantById(restaurantId);
            List<MenuItem> menuItems = menuDAO.getMenuByRestaurantId(restaurantId);
            List<String> categories = menuDAO.getCategoriesByRestaurant(restaurantId);

            request.setAttribute("restaurant", restaurant);
            request.setAttribute("menuItems", menuItems);
            request.setAttribute("categories", categories);
            request.getRequestDispatcher("/menu.jsp").forward(request, response);
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/restaurants");
        }
    }

    private void searchMenu(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String keyword = request.getParameter("keyword");
        List<MenuItem> menuItems;

        if (keyword != null && !keyword.trim().isEmpty()) {
            menuItems = menuDAO.searchMenuItems(keyword.trim());
            request.setAttribute("searchKeyword", keyword.trim());
        } else {
            menuItems = menuDAO.getAllMenuItems();
        }

        request.setAttribute("menuItems", menuItems);
        request.setAttribute("searchMode", true);
        request.getRequestDispatcher("/menu.jsp").forward(request, response);
    }

    private void filterByCategory(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String restaurantIdStr = request.getParameter("restaurantId");
        String category = request.getParameter("category");

        try {
            int restaurantId = Integer.parseInt(restaurantIdStr);
            Restaurant restaurant = restaurantDAO.getRestaurantById(restaurantId);
            List<String> categories = menuDAO.getCategoriesByRestaurant(restaurantId);
            List<MenuItem> menuItems;

            if (category != null && !category.trim().isEmpty() && !category.equals("all")) {
                menuItems = menuDAO.getMenuByCategory(restaurantId, category.trim());
                request.setAttribute("selectedCategory", category.trim());
            } else {
                menuItems = menuDAO.getMenuByRestaurantId(restaurantId);
            }

            request.setAttribute("restaurant", restaurant);
            request.setAttribute("menuItems", menuItems);
            request.setAttribute("categories", categories);
            request.getRequestDispatcher("/menu.jsp").forward(request, response);
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/restaurants");
        }
    }
}
