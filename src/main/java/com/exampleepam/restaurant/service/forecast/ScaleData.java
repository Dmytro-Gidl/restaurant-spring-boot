package com.exampleepam.restaurant.service.forecast;

import java.util.List;

public record ScaleData(List<String> labels, List<Integer> actual, List<Integer> forecast) {}
