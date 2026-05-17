package com.foodapp.controller;

import com.foodapp.dao.UserDAO;
import com.foodapp.model.User;
import com.foodapp.util.PasswordUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * LoginServlet — Handles user login authentication.
 * GET: Forwards to login.jsp
 * POST: Authenticates credentials and creates session.
 */
public class LoginServlet extends HttpServlet {

    private UserDAO userDAO;

    @Override
    public void init() throws ServletException {
        userDAO = new UserDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // If already logged in, redirect to home
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            response.sendRedirect(request.getContextPath() + "/home.jsp");
            return;
        }
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            String email = request.getParameter("email");
            String password = request.getParameter("password");

            // Server-side validation
            if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                request.setAttribute("error", "Please enter both email and password.");
                request.getRequestDispatcher("/login.jsp").forward(request, response);
                return;
            }

            email = email.trim().toLowerCase();

            // Look up user by email
            User user = userDAO.getUserByEmail(email);

            if (user != null && PasswordUtil.checkPassword(password, user.getPassword())) {
                // Login success — create session
                HttpSession session = request.getSession(true);
                session.setAttribute("user", user);
                session.setAttribute("userId", user.getUserId());
                session.setAttribute("userName", user.getFullName());
                session.setAttribute("userRole", user.getRole());
                session.setMaxInactiveInterval(30 * 60);  // 30 minutes

                // Redirect based on role
                if (user.isAdmin()) {
                    response.sendRedirect(request.getContextPath() + "/admin?action=dashboard");
                } else {
                    response.sendRedirect(request.getContextPath() + "/home.jsp");
                }
            } else {
                // Login failed
                request.setAttribute("error", "Invalid email or password.");
                request.setAttribute("email", email);
                request.getRequestDispatcher("/login.jsp").forward(request, response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "An internal error occurred. Please try again later.");
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        }
    }
}
