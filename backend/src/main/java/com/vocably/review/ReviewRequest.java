package com.vocably.review;

import jakarta.validation.constraints.NotNull;

public record ReviewRequest(@NotNull ReviewGrade grade) {
}
