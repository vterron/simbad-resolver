TargetResolver
==============

This is a simple Java interface, developed for the `PANIC instrument <https://w3.iaa.es/PANIC/>`_, to the `SIMBAD <http://simbad.u-strasbg.fr/simbad/>`_ name resolver. The database is queried and the information about the astronomical object, if found, returned as a TargetInformation instance::

    TargetResolver resolver = new TargetResolver();
    try {
        TargetInformation info = resolver.submit("M101");
        System.out.println(info);
    } catch (TargetNotFoundException e) {
        System.out.println("not found!");
    } catch (SIMBADQueryException e) {
        System.out.println("connection failed");
    }

