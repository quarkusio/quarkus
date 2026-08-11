package org.acme.pkginfo;

public class PkgInfoService {
    public String describe() {
        Package pkg = PkgInfoService.class.getPackage();
        PkgAnnotation anno = pkg.getAnnotation(PkgAnnotation.class);
        return anno != null ? anno.value() : "missing";
    }
}
