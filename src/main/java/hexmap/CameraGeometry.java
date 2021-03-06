package hexmap;

/**
 * Class describing the geometry of a telescopes camera.
 * Holds several public members with geometrical information about the camera.
 * Created by kbruegge on 2/13/17.
 */
public class CameraGeometry {

    /**
     * The number of pixels in this camera.
     */
    public int numberOfPixel;


    public double[] pixelXPositions;
    public double[] pixelYPositions;

    double pixelRotation;

    /**
     * The type of the pixel in this camera.
     */
    PixelType pixelType;


    /**
     * Array of id for the cameras pixel
     */
    public int[] pixelIds;

    /**
     * Surface area of each of the cameras pixels (the entrance window of the lightcone)
     */
    double[] pixelArea;

    /**
     * The rotation of the camera in its frame.
     */
    double cameraRotation;



    public int[][] neighbours;

    public String name;


    /**
     * The pixel type. Can either be of hexagonal or rectangular geometry.
     */
    public enum PixelType {
        RECTANGULAR("rectangular"),
        HEXAGONAL("hexagonal");

        String geometry;

        PixelType(String geometry) {
            this.geometry = geometry;
        }
    }

}
