package find.my.car.v1;

import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class RoutePathOverlay extends Overlay {
    private int _pathColor;
    private final List<GeoPoint> _points;

    public RoutePathOverlay(List<GeoPoint> points) {
            this(points, Color.RED);
    }

    public RoutePathOverlay(List<GeoPoint> points, int pathColor) {
            _points = points;
            _pathColor = pathColor;
    }

    public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
            Projection projection = mapView.getProjection();
            if (shadow == false && _points != null) {
                    Path path = new Path();
                    //We are creating the path
                    for (int i = 0; i < _points.size(); i++) {
                            GeoPoint gPointA = _points.get(i);
                            Point pointA = new Point();
                            projection.toPixels(gPointA, pointA);
                            if (i == 0) { 
                            	//This is the start point
                            	path.moveTo(pointA.x, pointA.y);
                            } else {
                            	path.lineTo(pointA.x, pointA.y);
                            }
                    }
                    Paint paint = new Paint();
                    paint.setAntiAlias(true);
                    paint.setColor(_pathColor);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(5);
                    paint.setAlpha(90);
                    if (!path.isEmpty())
                            canvas.drawPath(path, paint);
            }
            return super.draw(canvas, mapView, shadow, when);
    }
}