package si.um.feri.rri.system;

import si.um.feri.rri.component.MarkerComponent;

public class TrainAnimationSystem {

    private final MarkerComponent markers;

    public TrainAnimationSystem(MarkerComponent markers) {
        this.markers = markers;
    }

    public void update(float delta) {
        float speed = 0.05f;

        // Train 1
        if (markers.train1Forward) {
            markers.train1Progress += speed * delta;
            if (markers.train1Progress >= 1f) {
                markers.train1Progress = 1f;
                markers.train1Forward = false;
            }
        } else {
            markers.train1Progress -= speed * delta;
            if (markers.train1Progress <= 0f) {
                markers.train1Progress = 0f;
                markers.train1Forward = true;
            }
        }

        // Train 2
        if (markers.train2Forward) {
            markers.train2Progress += speed * delta;
            if (markers.train2Progress >= 1f) {
                markers.train2Progress = 1f;
                markers.train2Forward = false;
            }
        } else {
            markers.train2Progress -= speed * delta;
            if (markers.train2Progress <= 0f) {
                markers.train2Progress = 0f;
                markers.train2Forward = true;
            }
        }
    }
}
