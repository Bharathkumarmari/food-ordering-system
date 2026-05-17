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
 * RegisterServlet — Handles new user registration.
 * GET: Forwards to register.jsp
 * POST: Validates input, hashes password, creates user.
 */
public class RegisterServlet extends HttpServlet {

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
        request.getRequestDispatcher("/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        String phone = request.getParameter("phone");
        String address = request.getParameter("address");

        // Server-side validation
        StringBuilder errors = new StringBuilder();

        if (fullName == null || fullName.trim().isEmpty()) {
            errors.append("Full name is required. ");
        }
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            errors.append("Valid email is required. ");
        }
        if (password == null || password.length() < 6) {
            errors.append("Password must be at least 6 characters. ");
        }
        if (confirmPassword == null || !confirmPassword.equals(password)) {
            errors.append("Passwords do not match. ");
        }
        if (phone == null || phone.trim().length() < 10) {
            errors.append("Valid phone number is required. ");
        }

        if (errors.length() > 0) {
            request.setAttribute("error", errors.toString().trim());
            request.setAttribute("fullName", fullName);
            request.setAttribute("email", email);
            request.setAttribute("phone", phone);
            request.setAttribute("address", address);
            request.getRequestDispatcher("/register.jsp").forward(request, response);
            return;
        }

        email = email.trim().toLowerCase();

        // Check if email already exists
        if (userDAO.emailExists(email)) {
            request.setAttribute("error", "An account with this email already exists.");
            request.setAttribute("fullName", fullName);
            request.setAttribute("phone", phone);
            request.setAttribute("address", address);
            request.getRequestDispatcher("/register.jsp").forward(request, response);
            return;
        }

        // Hash password and create user
        String hashedPassword = PasswordUtil.hashPassword(password);
        User newUser = new User(fullName.trim(), email, hashedPassword, phone.trim(),
                                address != null ? address.trim() : "");

        boolean success = userDAO.registerUser(newUser);

        if (success) {
            // Auto-login after registration
            User registeredUser = userDAO.getUserByEmail(email);
            HttpSession session = request.getSession(true);
            session.setAttribute("user", registeredUser);
            session.setAttribute("userId", registeredUser.getUserId());
            session.setAttribute("userName", registeredUser.getFullName());
            session.setAttribute("userRole", registeredUser.getRole());
            session.setMaxInactiveInterval(30 * 60);

            response.sendRedirect(request.getContextPath() + "/home.jsp");
        } else {
            request.setAttribute("error", "Registration failed. Please try again.");
            request.getRequestDispatcher("/register.jsp").forward(request, response);
        }
    }
}
