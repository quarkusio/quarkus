package io.quarkus.tck.atinject;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.inject.build.compatible.spi.Discovery;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.inject.build.compatible.spi.MetaAnnotations;
import jakarta.enterprise.inject.build.compatible.spi.ScannedClasses;
import jakarta.enterprise.inject.literal.NamedLiteral;

import org.atinject.tck.auto.Convertible;
import org.atinject.tck.auto.Drivers;
import org.atinject.tck.auto.DriversSeat;
import org.atinject.tck.auto.FuelTank;
import org.atinject.tck.auto.Seat;
import org.atinject.tck.auto.Tire;
import org.atinject.tck.auto.V8Engine;
import org.atinject.tck.auto.accessories.Cupholder;
import org.atinject.tck.auto.accessories.SpareTire;

public class AtInjectTckExtension implements BuildCompatibleExtension {
    @Discovery
    public void discovery(ScannedClasses scan, MetaAnnotations meta) {
        scan.add(Convertible.class.getName());
        scan.add(DriversSeat.class.getName());
        scan.add(FuelTank.class.getName());
        scan.add(Seat.class.getName());
        scan.add(Tire.class.getName());
        scan.add(V8Engine.class.getName());

        scan.add(Cupholder.class.getName());
        scan.add(SpareTire.class.getName());
    }

    @Enhancement(types = Convertible.class)
    public void convertible(ClassConfig clazz) {
        clazz.fields()
                .stream()
                .filter(it -> "spareTire".equals(it.info().name()))
                .forEach(it -> it.addAnnotation(Spare.class));
    }

    @Enhancement(types = DriversSeat.class)
    public void driversSeat(ClassConfig clazz) {
        clazz.addAnnotation(Drivers.class);
    }

    @Enhancement(types = SpareTire.class)
    public void spareTire(ClassConfig clazz) {
        clazz.addAnnotation(NamedLiteral.of("spare"))
                .addAnnotation(Spare.class);
    }
}
