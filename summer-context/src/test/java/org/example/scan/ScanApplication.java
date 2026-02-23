package org.example.scan;

import org.example.annotation.Component;
import org.example.annotation.ComponentScan;
import org.example.annotation.Import;
import org.example.imported.LocalDateConfiguration;
import org.example.imported.ZonedDateConfiguration;

@ComponentScan
@Import({LocalDateConfiguration.class, ZonedDateConfiguration.class})
public class ScanApplication {
}
