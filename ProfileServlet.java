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
 * ProfileServlet — Handles user profile view and update.
 */
public class ProfileServlet extends HttpServlet {

    private UserDAO userDAO;

    @Override
    public void init() throws ServletException {
        userDAO = new UserDAO();
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
        // Refresh user data from DB
        User freshUser = userDAO.getUserById(user.getUserId());
        if (freshUser != null) {
            request.setAttribute("profileUser", freshUser);
        }
        request.getRequestDispatcher("/profile.jsp").forward(request, response);
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

        if ("updateProfile".equals(action)) {
            user.setFullName(request.getParameter("fullName"));
            user.setPhone(request.getParameter("phone"));
            user.setAddress(request.getParameter("address"));

            boolean success = userDAO.updateUser(user);
            if (success) {
                // Update session data
                session.setAttribute("user", user);
                session.setAttribute("userName", user.getFullName());
                response.sendRedirect(request.getContextPath() + "/profile?success=Profile+updated+successfully");
            } else {
                response.sendRedirect(request.getContextPath() + "/profile?error=Failed+to+update+profile");
            }
        } else if ("changePassword".equals(action)) {
            String currentPassword = request.getParameter("currentPassword");
            String newPassword = request.getParameter("newPassword");
            String confirmPassword = request.getParameter("confirmPassword");

            if (!PasswordUtil.checkPassword(currentPassword, user.getPassword())) {
                response.sendRedirect(request.getContextPath() + "/profile?error=Current+password+is+incorrect");
                return;
            }
            if (newPassword == null || newPassword.length() < 6) {
                response.sendRedirect(request.getContextPath() + "/profile?error=New+password+must+be+at+least+6+characters");
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                response.sendRedirect(request.getContextPath() + "/profile?error=Passwords+do+not+match");
                return;
            }

            String hashedPassword = PasswordUtil.hashPassword(newPassword);
            boolean success = userDAO.updatePassword(user.getUserId(), hashedPassword);
            if (success) {
                user.setPassword(hashedPassword);
                session.setAttribute("user", user);
                response.sendRedirect(request.getContextPath() + "/profile?success=Password+changed+successfully");
            } else {
                response.sendRedirect(request.getContextPath() + "/profile?error=Failed+to+change+password");
            }
        } else {
            response.sendRedirect(request.getContextPath() + "/profile");
        }
    }
}
