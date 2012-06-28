TargetResolver
==============

This is a simple Java interface, developed for the `PANIC instrument <https://w3.iaa.es/PANIC/>`_, to the `SIMBAD <http://simbad.u-strasbg.fr/simbad/>`_ name resolver. The database is queried and the information about the astronomical object, if found, returned as a TargetInformation instance. This object, in turn, can be used to plot, using `Staralt <http://catserver.ing.iac.es/staralt/>`_, the altitude of the object against time in a particular night. By default, altitudes are plotted for tonight and the `Calar Alto Observatory <http://www.caha.es/>`_, where PANIC is to be commissioned.

Example: M101
-------------

Fetch the information of the `Pinwheel Galaxy <http://en.wikipedia.org/wiki/Pinwheel_Galaxy>`_, and download its Staralt plot::

    try {
        TargetResolver resolver = new TargetResolver();
        TargetInformation info = resolver.submit("M101");
        System.out.println(info);

        Staralt staralt = new Staralt();
        File plot = staralt.plot(info); /* At CAHA, today */
        System.out.println();
        System.out.println("Staralt plot saved to: " + plot.getAbsolutePath());

        /* Do stuff with the plot */

        plot.deleteOnExit(); /* don't clutter the temporary directory */

    } catch (TargetNotFoundException e) {
        System.out.println("not found!");
    } catch (SIMBADQueryException e) {
        System.out.println("connection failed");


The output would look something like this::

    Name: M101
    Type: Interacting Galaxies
    RA: 14 03 12.51 (210.802120)
    DEC: +54 20 53.1 (54.348080)
    Epoch: J2012 | Equinox: 2000
    Reference system: ICRS
    Proper motions: null null

    Staralt plot saved to: /tmp/staralt_4441903800833962906.gif
