package com.foodapp.controller;

import com.foodapp.dao.*;
import com.foodapp.model.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

/**
 * AdminServlet — Handles all admin dashboard operations.
 * Supports: dashboard stats, restaurant CRUD, menu CRUD, order management.
 */
public class AdminServlet extends HttpServlet {

    private UserDAO userDAO;
    private RestaurantDAO restaurantDAO;
    private MenuDAO menuDAO;
    private OrderDAO orderDAO;

    @Override
    public void init() throws ServletException {
        userDAO = new UserDAO();
        restaurantDAO = new RestaurantDAO();
        menuDAO = new MenuDAO();
        orderDAO = new OrderDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        if (action == null) action = "dashboard";

        switch (action) {
            case "restaurants":
                manageRestaurants(request, response);
                break;
            case "menuItems":
                manageMenuItems(request, response);
                break;
            case "orders":
                manageOrders(request, response);
                break;
            case "users":
                manageUsers(request, response);
                break;
            case "dashboard":
            default:
                showDashboard(request, response);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        if (action == null) {
            response.sendRedirect(request.getContextPath() + "/admin?action=dashboard");
            return;
        }

        switch (action) {
            case "addRestaurant":
                addRestaurant(request, response);
                break;
            case "updateRestaurant":
                updateRestaurant(request, response);
                break;
            case "deleteRestaurant":
                deleteRestaurant(request, response);
                break;
            case "addMenuItem":
                addMenuItem(request, response);
                break;
            case "updateMenuItem":
                updateMenuItem(request, response);
                break;
            case "deleteMenuItem":
                deleteMenuItem(request, response);
                break;
            case "updateOrderStatus":
                updateOrderStatus(request, response);
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/admin?action=dashboard");
        }
    }

    // ==================== Dashboard ====================

    private void showDashboard(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int totalUsers = userDAO.getTotalUserCount();
        int totalRestaurants = restaurantDAO.getTotalRestaurantCount();
        int totalOrders = orderDAO.getTotalOrderCount();
        double totalRevenue = orderDAO.getTotalRevenue();

        request.setAttribute("totalUsers", totalUsers);
        request.setAttribute("totalRestaurants", totalRestaurants);
        request.setAttribute("totalOrders", totalOrders);
        request.setAttribute("totalRevenue", totalRevenue);

        // Recent orders for dashboard
        List<Order> recentOrders = orderDAO.getAllOrders();
        request.setAttribute("recentOrders", recentOrders);

        request.setAttribute("adminSection", "dashboard");
        request.getRequestDispatcher("/admin.jsp").forward(request, response);
    }

    // ==================== Restaurant Management ====================

    private void manageRestaurants(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<Restaurant> restaurants = restaurantDAO.getAllRestaurantsAdmin();
        request.setAttribute("restaurants", restaurants);
        request.setAttribute("adminSection", "restaurants");
        request.getRequestDispatcher("/admin.jsp").forward(request, response);
    }

    private void addRestaurant(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Restaurant r = new Restaurant();
        r.setRestaurantName(request.getParameter("restaurantName"));
        r.setCuisineType(request.getParameter("cuisineType"));
        r.setLocation(request.getParameter("location"));
        r.setDeliveryTime(request.getParameter("deliveryTime"));
        r.setImageUrl(request.getParameter("imageUrl"));

        restaurantDAO.addRestaurant(r);
        response.sendRedirect(request.getContextPath() + "/admin?action=restaurants&success=Restaurant+added");
    }

    private void updateRestaurant(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Restaurant r = new Restaurant();
        r.setRestaurantId(Integer.parseInt(request.getParameter("restaurantId")));
        r.setRestaurantName(request.getParameter("restaurantName"));
        r.setCuisineType(request.getParameter("cuisineType"));
        r.setLocation(request.getParameter("location"));
        r.setDeliveryTime(request.getParameter("deliveryTime"));
        r.setActive("true".equals(request.getParameter("isActive")));
        r.setImageUrl(request.getParameter("imageUrl"));

        restaurantDAO.updateRestaurant(r);
        response.sendRedirect(request.getContextPath() + "/admin?action=restaurants&success=Restaurant+updated");
    }

    private void deleteRestaurant(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int id = Integer.parseInt(request.getParameter("restaurantId"));
        restaurantDAO.deleteRestaurant(id);
        response.sendRedirect(request.getContextPath() + "/admin?action=restaurants&success=Restaurant+deleted");
    }

    // ==================== Menu Item Management ====================

    private void manageMenuItems(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<MenuItem> menuItems = menuDAO.getAllMenuItems();
        List<Restaurant> restaurants = restaurantDAO.getAllRestaurantsAdmin();
        request.setAttribute("menuItems", menuItems);
        request.setAttribute("restaurants", restaurants);
        request.setAttribute("adminSection", "menuItems");
        request.getRequestDispatcher("/admin.jsp").forward(request, response);
    }

    private void addMenuItem(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        MenuItem item = new MenuItem();
        item.setRestaurantId(Integer.parseInt(request.getParameter("restaurantId")));
        item.setItemName(request.getParameter("itemName"));
        item.setDescription(request.getParameter("description"));
        item.setPrice(Double.parseDouble(request.getParameter("price")));
        item.setCategory(request.getParameter("category"));
        item.setVeg("true".equals(request.getParameter("isVeg")));
        item.setImageUrl(request.getParameter("imageUrl"));

        menuDAO.addMenuItem(item);
        response.sendRedirect(request.getContextPath() + "/admin?action=menuItems&success=Menu+item+added");
    }

    private void updateMenuItem(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        MenuItem item = new MenuItem();
        item.setMenuId(Integer.parseInt(request.getParameter("menuId")));
        item.setItemName(request.getParameter("itemName"));
        item.setDescription(request.getParameter("description"));
        item.setPrice(Double.parseDouble(request.getParameter("price")));
        item.setCategory(request.getParameter("category"));
        item.setVeg("true".equals(request.getParameter("isVeg")));
        item.setAvailable("true".equals(request.getParameter("isAvailable")));
        item.setImageUrl(request.getParameter("imageUrl"));

        menuDAO.updateMenuItem(item);
        response.sendRedirect(request.getContextPath() + "/admin?action=menuItems&success=Menu+item+updated");
    }

    private void deleteMenuItem(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int id = Integer.parseInt(request.getParameter("menuId"));
        menuDAO.deleteMenuItem(id);
        response.sendRedirect(request.getContextPath() + "/admin?action=menuItems&success=Menu+item+deleted");
    }

    // ==================== Order Management ====================

    private void manageOrders(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<Order> orders = orderDAO.getAllOrders();
        request.setAttribute("allOrders", orders);
        request.setAttribute("adminSection", "orders");
        request.getRequestDispatcher("/admin.jsp").forward(request, response);
    }

    private void updateOrderStatus(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int orderId = Integer.parseInt(request.getParameter("orderId"));
        String status = request.getParameter("status");
        orderDAO.updateOrderStatus(orderId, status);
        response.sendRedirect(request.getContextPath() + "/admin?action=orders&success=Order+status+updated");
    }

    // ==================== User Management ====================

    private void manageUsers(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<User> users = userDAO.getAllUsers();
        request.setAttribute("allUsers", users);
        request.setAttribute("adminSection", "users");
        request.getRequestDispatcher("/admin.jsp").forward(request, response);
    }
}
