package com.foodapp.controller;

import com.foodapp.dao.OrderDAO;
import com.foodapp.model.Order;
import com.foodapp.model.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

/**
 * OrderServlet — Handles order placement, history, and tracking.
 */
public class OrderServlet extends HttpServlet {

    private OrderDAO orderDAO;

    @Override
    public void init() throws ServletException {
        orderDAO = new OrderDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        User user = (User) session.getAttribute("user");
        String action = request.getParameter("action");
        if (action == null) action = "history";

        switch (action) {
            case "track":
                trackOrder(request, response);
                break;
            case "history":
            default:
                orderHistory(request, response, user);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        User user = (User) session.getAttribute("user");
        String action = request.getParameter("action");

        if ("place".equals(action)) {
            placeOrder(request, response, user);
        } else {
            response.sendRedirect(request.getContextPath() + "/orders?action=history");
        }
    }

    private void placeOrder(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {

        String deliveryAddress = request.getParameter("deliveryAddress");
        String paymentMethod = request.getParameter("paymentMethod");

        if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
            deliveryAddress = user.getAddress();
        }
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            paymentMethod = "CASH_ON_DELIVERY";
        }

        int orderId = orderDAO.placeOrder(user.getUserId(), deliveryAddress.trim(), paymentMethod);

        if (orderId > 0) {
            response.sendRedirect(request.getContextPath() + "/orders?action=track&orderId=" + orderId + "&success=Order+placed+successfully!");
        } else {
            response.sendRedirect(request.getContextPath() + "/cart?error=Failed+to+place+order.+Your+cart+may+be+empty.");
        }
    }

    private void orderHistory(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        List<Order> orders = orderDAO.getOrdersByUserId(user.getUserId());
        request.setAttribute("orders", orders);
        request.getRequestDispatcher("/orders.jsp").forward(request, response);
    }

    private void trackOrder(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            Order order = orderDAO.getOrderById(orderId);

            if (order != null) {
                request.setAttribute("order", order);
                request.setAttribute("trackingMode", true);
            }

            // Also load full history
            HttpSession session = request.getSession(false);
            User user = (User) session.getAttribute("user");
            List<Order> orders = orderDAO.getOrdersByUserId(user.getUserId());
            request.setAttribute("orders", orders);
            request.getRequestDispatcher("/orders.jsp").forward(request, response);
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/orders?action=history");
        }
    }
}
