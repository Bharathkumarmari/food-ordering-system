package com.foodapp.controller;

import com.foodapp.dao.RestaurantDAO;
import com.foodapp.model.Restaurant;

import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * RestaurantServlet — Handles restaurant listing, search, and filtering.
 */
public class RestaurantServlet extends HttpServlet {

    private RestaurantDAO restaurantDAO;

    @Override
    public void init() throws ServletException {
        restaurantDAO = new RestaurantDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        String search = request.getParameter("search");
        String cuisine = request.getParameter("cuisine");

        if ("view".equals(action)) {
            viewRestaurant(request, response);
            return;
        }

        if ("nearby".equals(action)) {
            getNearbyRestaurants(request, response);
            return;
        }

        if (search != null && !search.trim().isEmpty()) {
            // Set keyword for searchRestaurants method to use
            request.setAttribute("searchKeyword", search.trim());
            searchRestaurants(request, response, search.trim());
        } else if (cuisine != null && !cuisine.trim().isEmpty() && !"all".equals(cuisine)) {
            filterByCuisine(request, response, cuisine.trim());
        } else {
            listRestaurants(request, response);
        }
    }

    private void getNearbyRestaurants(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        try {
            double lat = Double.parseDouble(request.getParameter("lat"));
            double lng = Double.parseDouble(request.getParameter("lng"));
            
            List<Restaurant> restaurants = restaurantDAO.getNearbyRestaurants(lat, lng);
            System.out.println("[DEBUG] getNearbyRestaurants: lat=" + lat + ", lng=" + lng + ", found=" + restaurants.size() + " restaurants");
            
            String json = new Gson().toJson(restaurants);
            response.getWriter().write(json);
        } catch (Exception e) {
            System.err.println("[RestaurantServlet] Error in getNearbyRestaurants: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Invalid coordinates or system error.\"}");
        }
    }

    private void listRestaurants(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<Restaurant> restaurants = restaurantDAO.getAllRestaurants();
        List<String> cuisines = restaurantDAO.getAllCuisineTypes();

        request.setAttribute("restaurants", restaurants);
        request.setAttribute("cuisines", cuisines);
        request.getRequestDispatcher("/restaurants.jsp").forward(request, response);
    }

    private void searchRestaurants(HttpServletRequest request, HttpServletResponse response, String keyword)
            throws ServletException, IOException {
        List<Restaurant> restaurants;

        if (keyword != null && !keyword.trim().isEmpty()) {
            restaurants = restaurantDAO.searchRestaurants(keyword.trim());
            request.setAttribute("searchKeyword", keyword.trim());
        } else {
            restaurants = restaurantDAO.getAllRestaurants();
        }

        List<String> cuisines = restaurantDAO.getAllCuisineTypes();
        request.setAttribute("restaurants", restaurants);
        request.setAttribute("cuisines", cuisines);
        request.getRequestDispatcher("/restaurants.jsp").forward(request, response);
    }

    private void filterByCuisine(HttpServletRequest request, HttpServletResponse response, String cuisine)
            throws ServletException, IOException {
        List<Restaurant> restaurants;

        if (cuisine != null && !cuisine.trim().isEmpty() && !cuisine.equals("all")) {
            restaurants = restaurantDAO.getRestaurantsByCuisine(cuisine.trim());
            request.setAttribute("selectedCuisine", cuisine.trim());
        } else {
            restaurants = restaurantDAO.getAllRestaurants();
        }

        List<String> cuisines = restaurantDAO.getAllCuisineTypes();
        request.setAttribute("restaurants", restaurants);
        request.setAttribute("cuisines", cuisines);
        request.getRequestDispatcher("/restaurants.jsp").forward(request, response);
    }

    private void viewRestaurant(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            int restaurantId = Integer.parseInt(request.getParameter("id"));
            Restaurant restaurant = restaurantDAO.getRestaurantById(restaurantId);

            if (restaurant != null) {
                request.setAttribute("restaurant", restaurant);
                // Forward to menu page to show restaurant's menu
                response.sendRedirect(request.getContextPath() + "/menu?restaurantId=" + restaurantId);
            } else {
                response.sendRedirect(request.getContextPath() + "/restaurants?error=Restaurant+not+found");
            }
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/restaurants");
        }
    }
}
