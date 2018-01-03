/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.task.projectanalysis.qualitymodel;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.sonar.api.config.Configuration;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.sonar.api.CoreProperties.RATING_GRID;
import static org.sonar.api.CoreProperties.RATING_GRID_DEF_VALUES;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating.A;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating.B;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating.C;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating.D;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating.E;

public class DebtRatingGrid {

  private final double[] gridValues;
  private final List<Function<Double, Rating>> gridFunctions;

  public DebtRatingGrid(Configuration config) {
    try {
      String[] grades = config.getStringArray(RATING_GRID);
      gridValues = new double[4];
      for (int i = 0; i < 4; i++) {
        gridValues[i] = Double.parseDouble(grades[i]);
      }
      this.gridFunctions = buildGridFunctions(gridValues);
    } catch (Exception e) {
      throw new IllegalArgumentException("The rating grid is incorrect. Expected something similar to '"
        + RATING_GRID_DEF_VALUES + "' and got '" + config.get(RATING_GRID).orElse("") + "'", e);
    }
  }

  public DebtRatingGrid(double[] gridValues) {
    this.gridValues = Arrays.copyOf(gridValues, gridValues.length);
    this.gridFunctions = buildGridFunctions(gridValues);
  }

  private static List<Function<Double, Rating>> buildGridFunctions(double[] gridValues) {
    List<Function<Double, Rating>> gridFunctions = new ArrayList<>();
    checkState(gridValues.length == 4, "Rating grid should contains 4 values");
    gridFunctions.add(value -> value == 0 ? A : null);
    gridFunctions.add(value -> isBetween(value, 0, gridValues[0]) ? A : null);
    gridFunctions.add(value -> isBetween(value, gridValues[0], gridValues[1]) ? B : null);
    gridFunctions.add(value -> isBetween(value, gridValues[1], gridValues[2]) ? C : null);
    gridFunctions.add(value -> isBetween(value, gridValues[2], gridValues[3]) ? D : null);
    gridFunctions.add(value -> value > gridValues[3] ? E : null);
    return gridFunctions;
  }

  public Rating getRatingForDensity(double value) {
    return gridFunctions.stream()
      .map(f -> f.apply(value))
      .filter(Objects::nonNull)
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException(format("Invalid value '%s'", value)));
  }

  public double getGradeLowerBound(Rating rating) {
    if (rating.getIndex() > 1) {
      return gridValues[rating.getIndex() - 2];
    }
    return 0;
  }

  @VisibleForTesting
  double[] getGridValues() {
    return gridValues;
  }

  private static boolean isBetween(double value, double min, double max) {
    return value > min && value <= max;
  }

}
