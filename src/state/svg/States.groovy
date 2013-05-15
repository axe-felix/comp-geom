package state.svg

@Grab(group = 'org.apache.xmlgraphics', module = 'batik-parser', version = '1.7')
import groovy.transform.CompileStatic
import org.apache.batik.parser.AWTPathProducer
import org.apache.batik.parser.PathHandler
import org.apache.batik.parser.PathParser

import java.awt.*
import java.awt.geom.PathIterator
import java.text.ParseException
import java.util.List

println "slurping svg..."
def svg = new XmlSlurper().parse(new File("./states.svg"))

println "extracting states..."
Map<String, Shape> states = svg.g.path.findAll().collectEntries {
    [(it.@id): svgDataToShape(it.@d as String)]
}

def statePaths = states.collectEntries {
    [(it.key): convertShapeToPathsList(it.value)]
}

statePaths.each {
    println "Area of $it.key is ${getAreaInSqKm(it.value).round(2)} km2"
}

println "extracting cities..."
def cities = svg.path.findAll().collectEntries {
    [(it.@id): [it.@"sodipodi:cx".text() as double, it.@"sodipodi:cy".text() as double]]
}

states.each { state ->
    cities.each { city ->
        if (state.value.contains(city.value[0], city.value[1])) {
            println "$city.key ist in $state.key"
        }
    }
}

@CompileStatic
public Shape svgDataToShape(String s) throws ParseException {
    PathParser pp = new PathParser();
    PathHandler awtPathProducer = new AWTPathProducer()
    pp.setPathHandler(awtPathProducer);
    pp.parse(s);
    awtPathProducer.shape
}

@CompileStatic
private List<List<double[]>> convertShapeToPathsList(Shape stateShape) {
    def state = []
    def polygonOfState = []
    for (PathIterator pi = stateShape.getPathIterator(null); !pi.isDone(); pi.next()) {
        double[] coords = new double[2];
        int segmentType = pi.currentSegment(coords);
        if (!segmentType.equals(PathIterator.SEG_CLOSE)) {
            polygonOfState << coords
        } else {
            state << polygonOfState
            polygonOfState = []
        }
    }
    return state
}

@CompileStatic
public double getAreaInSqKm(List<List<double[]>> state) {
    double sum = 0
    state.eachWithIndex { List<double[]> polygon, int i1 ->
        double sumPolygon = 0
        for (int i = -1; i < polygon.size() - 1; i++) {
            sumPolygon += ((double) polygon[i][1]) * (polygon[i - 1][0] - polygon[i + 1][0]) / 2;
        }
        sumPolygon = sumPolygon.abs()
        if (isHole(polygon, state)) {
            sum -= sumPolygon
        } else {
            sum += sumPolygon
        }
    }
    // factor to convert result in actual km²
    return sum / 0.85d
}

boolean isHole(List<double[]> polygon, List<List<double[]>> state) {
    for (List<double[]> currentPolygon : state) {
        if (!polygon.is(currentPolygon)) {
            if (isPointInPolygon(currentPolygon, polygon[0]))
                true
        }
    }
    false
}

//double[] getBoundingBox(List<double[]> polygon) {
//    return new double[0];  //To change body of created methods use File | Settings | File Templates.
//}
boolean isPointInPolygon(List<double[]> polygon, double[] point) {
    return false;  //To change body of created methods use File | Settings | File Templates.
}