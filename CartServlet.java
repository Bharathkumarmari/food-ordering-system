package com.foodapp.controller;

import com.foodapp.dao.CartDAO;
import com.foodapp.model.Cart;
import com.foodapp.model.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * CartServlet — Handles cart operations (add, remove, update, view, clear).
 * Supports both regular requests and AJAX (JSON) responses.
 */
public class CartServlet extends HttpServlet {

    private CartDAO cartDAO;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        cartDAO = new CartDAO();
        gson = new Gson();
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

        if ("count".equals(action)) {
            // AJAX request for cart item count
            int count = cartDAO.getCartItemCount(user.getUserId());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();
            JsonObject json = new JsonObject();
            json.addProperty("count", count);
            out.print(gson.toJson(json));
            out.flush();
            return;
        }

        // Regular page load — show cart
        Cart cart = cartDAO.getOrCreateCart(user.getUserId());
        request.setAttribute("cart", cart);
        request.getRequestDispatcher("/cart.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            sendJsonResponse(response, false, "Please login first");
            return;
        }

        User user = (User) session.getAttribute("user");
        String action = request.getParameter("action");

        if (action == null) {
            sendJsonResponse(response, false, "Invalid action");
            return;
        }

        Cart cart = cartDAO.getOrCreateCart(user.getUserId());
        boolean success = false;
        String message = "";

        switch (action) {
            case "add":
                try {
                    int menuId = Integer.parseInt(request.getParameter("menuId"));
                    int quantity = 1;
                    String qtyStr = request.getParameter("quantity");
                    if (qtyStr != null && !qtyStr.isEmpty()) {
                        quantity = Integer.parseInt(qtyStr);
                    }
                    success = cartDAO.addToCart(cart.getCartId(), menuId, quantity);
                    message = success ? "Item added to cart!" : "Failed to add item";
                } catch (NumberFormatException e) {
                    message = "Invalid item";
                }
                break;

            case "update":
                try {
                    int cartItemId = Integer.parseInt(request.getParameter("cartItemId"));
                    int qty = Integer.parseInt(request.getParameter("quantity"));
                    success = cartDAO.updateCartItemQuantity(cartItemId, qty);
                    message = success ? "Cart updated!" : "Failed to update cart";
                } catch (NumberFormatException e) {
                    message = "Invalid request";
                }
                break;

            case "remove":
                try {
                    int cartItemId = Integer.parseInt(request.getParameter("cartItemId"));
                    success = cartDAO.removeCartItem(cartItemId);
                    message = success ? "Item removed from cart!" : "Failed to remove item";
                } catch (NumberFormatException e) {
                    message = "Invalid item";
                }
                break;

            case "clear":
                success = cartDAO.clearCart(cart.getCartId());
                message = success ? "Cart cleared!" : "Failed to clear cart";
                break;

            default:
                message = "Unknown action";
        }

        // Check if AJAX request
        String xRequestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equals(xRequestedWith)) {
            // Reload cart data for the response
            cart = cartDAO.getOrCreateCart(user.getUserId());
            JsonObject json = new JsonObject();
            json.addProperty("success", success);
            json.addProperty("message", message);
            json.addProperty("cartCount", cart.getTotalItems());
            json.addProperty("cartTotal", cart.getTotalPrice());
            sendJsonDirect(response, json);
        } else {
            // Regular form submit — redirect to cart page
            response.sendRedirect(request.getContextPath() + "/cart");
        }
    }

    private void sendJsonResponse(HttpServletResponse response, boolean success, String message) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        JsonObject json = new JsonObject();
        json.addProperty("success", success);
        json.addProperty("message", message);
        out.print(gson.toJson(json));
        out.flush();
    }

    private void sendJsonDirect(HttpServletResponse response, JsonObject json) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.print(gson.toJson(json));
        out.flush();
    }
}
