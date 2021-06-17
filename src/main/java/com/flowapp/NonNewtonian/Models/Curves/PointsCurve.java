package com.flowapp.NonNewtonian.Models.Curves;

import com.flowapp.NonNewtonian.Models.Point;

import java.util.Arrays;
import java.util.List;

public class PointsCurve implements Curve {

    protected final Point[] mPoints;

    public PointsCurve(Point[] mPoints) {
        Arrays.sort(mPoints, new Point.SortbyXY());
        this.mPoints = mPoints;
    }

    public PointsCurve(List<Point> points) {
        this(points.toArray(new Point[0]));
    }

    public Point[] getPoints() {
        return mPoints;
    }

    @Override
    public Float getY(float x) {
        Arrays.sort(mPoints, new Point.SortbyXY());
        Point lastFoundPoint = mPoints[0];
        float newValue = lastFoundPoint.getY();
        for (Point curveEntry: mPoints) {
            float foundX = curveEntry.getX();
            float foundY = curveEntry.getY();
            Point thisPoint = new Point(foundX, foundY);
            newValue = foundY;
            if (foundX < x) {
                lastFoundPoint = thisPoint;
            } else if (foundX == x) {
                break;
            } else {
                if (foundX == lastFoundPoint.getX()) {
                    break;
                }
                newValue = lastFoundPoint.getY() + getPercentage(lastFoundPoint.getX(), foundX, x) * (foundY - lastFoundPoint.getY());
                break;
            }
        }
        return newValue;
    }

    float getPercentage(float start, float end, float value) {
        return (value - start) / (end - start);
    }

    @Override
    public boolean inRange(float x) {
        return true;
    }

    @Override
    public Float getX(float y) {
        Arrays.sort(mPoints, new Point.SortbyY());
        Point lastFoundPoint = mPoints[0];
        float newValue = lastFoundPoint.getX();
        for (Point curveEntry: mPoints) {
            float foundX = curveEntry.getX();
            float foundY = curveEntry.getY();
            Point thisPoint = new Point(foundX, foundY);
            newValue = foundX;
            if (foundY < y) {
                lastFoundPoint = thisPoint;
            } else if (foundY == y) {
                break;
            } else {
                newValue = lastFoundPoint.getX() + getPercentage (lastFoundPoint.getY(), foundY,y) * (foundX - lastFoundPoint.getX());
                break;
            }
        }
        return newValue;
    }
}
