import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import hexmap.TelescopeArray;
import io.CSVWriter;
import io.ImageReader;
import ml.TreeEnsemblePredictor;
import ml.Vectorizer;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reconstruction.DirectionReconstruction;
import reconstruction.HillasParametrization;
import reconstruction.TailCut;
import reconstruction.containers.Moments;
import reconstruction.containers.ReconstrucedEvent;
import reconstruction.containers.ShowerImage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertTrue;

/**
 *
 * Created by mackaiver on 09/08/17.
 */
public class AnalysisTest {

    static Logger log = LoggerFactory.getLogger(AnalysisTest.class);


    @Test
    public void testStereoParameters() throws IOException {
        URL url = ImageReader.class.getResource("/data/images.json.gz");

        ImageReader events = ImageReader.fromURL(url);

        for (ImageReader.Event event : events) {
            List<ShowerImage> showerImages = TailCut.onImagesInEvent(event);
            List<Moments> moments = HillasParametrization.fromShowerImages(showerImages);

            ReconstrucedEvent reconstrucedEvent = DirectionReconstruction.fromMoments(moments, event.mc.alt, event.mc.az);

            if (reconstrucedEvent.direction.isNaN()){
                continue;
            }

            assertTrue(reconstrucedEvent.direction.getZ() > 0);
        }

    }


    @Test
    public void testFullChain() throws IOException, URISyntaxException {
        URL url = ImageReader.class.getResource("/data/images.json.gz");
        URL predictorURL = ImageReader.class.getResource("/classifier.json");

        TemporaryFolder folder = new TemporaryFolder();
        folder.create();

        CSVWriter csv = new CSVWriter(folder.newFile());

        TreeEnsemblePredictor predictor = new TreeEnsemblePredictor(Paths.get(predictorURL.toURI()));
        ImageReader events = ImageReader.fromURL(url);


        for (ImageReader.Event event : events) {
            List<ShowerImage> showerImages = TailCut.onImagesInEvent(event);
            List<Moments> moments = HillasParametrization.fromShowerImages(showerImages);

            double prediction = moments.stream()
                    .map(m ->
                            new Vectorizer().of(
                                    event.array.numTriggeredTelescopes,
                                    m.numberOfPixel,
                                    m.width,
                                    m.length,
                                    m.skewness,
                                    m.kurtosis,
                                    m.phi,
                                    m.miss,
                                    m.size,
                                    TelescopeArray.cta().telescopeFromId(m.telescopeID).telescopeType.ordinal()
                            ).createFloatVector()
                    )
                    .mapToDouble(f ->
                            (double) predictor.predictProba(f)[0]
                    )
                    .average()
                    .orElse(0);

            ReconstrucedEvent reconstrucedEvent = DirectionReconstruction.fromMoments(moments, event.mc.alt, event.mc.az);

            if (reconstrucedEvent.direction.isNaN()){
                continue;
            }

            csv.append(reconstrucedEvent, prediction);
        }

    }

    @Test
    public void testMomentsStream() throws IOException {
        URL url = ImageReader.class.getResource("/data/images.json.gz");
        ImageReader events = ImageReader.fromURL(url);



        List<Moments> moments = events.stream()
                .flatMap(TailCut::streamShowerImages)
                .map(HillasParametrization::fromShowerImage)
                .filter(m -> m.size > 0.0)
                .collect(toList());

        assertTrue(moments.size() >= 2);

        moments.forEach(m -> {
            assertTrue(m.length > m.width);
        });

    }

    @Test
    public void testPrediction() throws IOException, URISyntaxException {
        ImageReader events = ImageReader.fromURL(ImageReader.class.getResource("/data/images.json.gz"));

        URL predictorURL = ImageReader.class.getResource("/classifier.json");

        TreeEnsemblePredictor predictor = new TreeEnsemblePredictor(Paths.get(predictorURL.toURI()));

        for (ImageReader.Event event : events) {
            List<ShowerImage> showerImages = TailCut.onImagesInEvent(event);
            List<Moments> moments = HillasParametrization.fromShowerImages(showerImages);

            int numberOfTelescopes = moments.size();

            double prediction = moments.stream()
                    .map(m ->
                            new Vectorizer().of(
                                    numberOfTelescopes,
                                    m.numberOfPixel,
                                    m.width,
                                    m.length,
                                    m.skewness,
                                    m.kurtosis,
                                    m.phi,
                                    m.miss,
                                    m.size,
                                    TelescopeArray.cta().telescopeFromId(m.telescopeID).telescopeType.ordinal()
                            ).createFloatVector()
                    )
                    .mapToDouble(f ->
                            (double) predictor.predictProba(f)[0]
                    )
                    .average()
                    .orElse(0);
        }

    }

    @Test
    public void testParallelStream() throws IOException, URISyntaxException {
        ImageReader events = ImageReader.fromURL(ImageReader.class.getResource("/data/images.json.gz"));
        List<ImageReader.Event> eventList = events.stream().collect(toList());

        URL predictorURL = ImageReader.class.getResource("/classifier.json");
        TreeEnsemblePredictor predictor = new TreeEnsemblePredictor(Paths.get(predictorURL.toURI()));

        Iterable<ImageReader.Event> cycle = Iterables.cycle(eventList);


        long N = 10000;
        Stopwatch stopwatch = Stopwatch.createStarted();

        List<Double> predictions = StreamSupport.stream(cycle.spliterator(), true)
                .limit(N)
                .map(event -> {
                    List<ShowerImage> showerImages = TailCut.onImagesInEvent(event);
                    List<Moments> moments = HillasParametrization.fromShowerImages(showerImages);

                    int numberOfTelescopes = moments.size();

                    return moments.stream()
                            .map(m ->
                                    new Vectorizer().of(
                                            numberOfTelescopes,
                                            m.numberOfPixel,
                                            m.width,
                                            m.length,
                                            m.skewness,
                                            m.kurtosis,
                                            m.phi,
                                            m.miss,
                                            m.size,
                                            TelescopeArray.cta().telescopeFromId(m.telescopeID).telescopeType.ordinal()
                                    ).createFloatVector()
                            )
                            .mapToDouble(f ->
                                    (double) predictor.predictProba(f)[0]
                            )
                            .average()
                            .orElse(0);


                })
                .collect(toList());


        Duration duration = stopwatch.elapsed();
        log.info("Reconstruced {} event in {} seconds. Thats {} events per second", N, duration.getSeconds(), N/duration.getSeconds());
    }

}
