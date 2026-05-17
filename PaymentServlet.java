package com.foodapp.controller;

import com.foodapp.dao.PaymentDAO;
import com.foodapp.model.Payment;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * PaymentServlet — Handles payment simulation and status checks.
 */
public class PaymentServlet extends HttpServlet {

    private PaymentDAO paymentDAO;

    @Override
    public void init() throws ServletException {
        paymentDAO = new PaymentDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String orderIdStr = request.getParameter("orderId");
        if (orderIdStr != null) {
            try {
                int orderId = Integer.parseInt(orderIdStr);
                Payment payment = paymentDAO.getPaymentByOrderId(orderId);
                request.setAttribute("payment", payment);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        request.getRequestDispatcher("/checkout.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Payment processing is handled within OrderServlet.placeOrder()
        // This servlet can be extended for advanced payment gateway integration.
        response.sendRedirect(request.getContextPath() + "/orders?action=history");
    }
}
