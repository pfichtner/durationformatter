# durationformatter [![Build Status](https://buildhive.cloudbees.com/job/pfichtner/job/durationformatter/badge/icon)](https://buildhive.cloudbees.com/job/pfichtner/job/durationformatter/)


Java Library minimal in size and without overhead just for formatting durations

## Intention
Because I could not find the 5th time i searched for an artifact just for formatting 
durations to strings, I started to write my own. This class should be flexible 
enough to cover all possible needs.

## Usage
These showcases are copied from the UnitTests:

Default digit format

```java
DurationFormatter df = DurationFormatter.DIGITS;
assertEquals("01:02:03", df.formatMillis(HOURS.toMillis(1) + MINUTES.toMillis(2) + SECONDS.toMillis(3)));
```		

Default symbol format

```java
DurationFormatter df = DurationFormatter.SYMBOLS;
assertEquals("792h 0min 33s", df.formatMillis(DAYS.toMillis(33) + SECONDS.toMillis(33)));
```

The amount of units can be limited using `maximumAmountOfUnitsToShow`. E.g. when having long running actions like downloads it makes no sense to generate texts like *2 days 0 hours 0 minutes 17 seconds*, the information *2 days 0 hours* might be enough but later, when there are only some minutes or seconds left, the text should be *x minutes* or *x seconds*, see this example

```java
DurationFormatter df = Builder.SYMBOLS.maximum(DAYS).minimum(SECONDS).suppressZeros(LEADING).maximumAmountOfUnitsToShow(2).build();

assertEquals("3d 0h", df.formatMillis(DAYS.toMillis(3)));
assertEquals("3d 0h", df.formatMillis(DAYS.toMillis(3) + SECONDS.toMillis(1)));
assertEquals("3d 2h", df.formatMillis(DAYS.toMillis(3) + HOURS.toMillis(2) + SECONDS.toMillis(1)));
assertEquals("3d 13h", df.formatMillis(DAYS.toMillis(3) + HOURS.toMillis(12) + MINUTES.toMillis(31) + SECONDS.toMillis(1)));
assertEquals("12h 31min", df.formatMillis(HOURS.toMillis(12) + MINUTES.toMillis(31) + SECONDS.toMillis(1)));
assertEquals("31min 0s", df.formatMillis(MINUTES.toMillis(31)));
assertEquals("31min 1s", df.formatMillis(MINUTES.toMillis(31) + SECONDS.toMillis(1)));
```

To create your customized DurationFormatter you can use one of the predefined Builders<br>
...there is one for digits...

```java
DurationFormatter df  = DurationFormatter.Builder.DIGITS.minimum(TimeUnit.NANOSECONDS).maximum(MILLISECONDS)
	.stripZeros(StripMode.LEADING).build();
assertEquals("000", df.format(0, TimeUnit.NANOSECONDS));
assertEquals("999", df.format(999, TimeUnit.NANOSECONDS));
assertEquals("001:499", df.format(1499, TimeUnit.NANOSECONDS));
```

...and one for symbols...

```java
DurationFormatter df = Builder.SYMBOLS.separator(", ").symbol(MINUTES, "m").valueSymbolSeparator(" ").build();
assertEquals("0 h, 0 m, 34 s", df.formatMillis(SECONDS.toMillis(33) + MILLISECONDS.toMillis(777)));
```

Of course you can create your own DurationFormatter without defaults by instantiating your own Builder

```java
DurationFormatter df = new Builder().maximum(DAYS).minimum(NANOSECONDS).separator("|").valueSymbolSeparator("_")
	.symbol(HOURS, "hours").symbol(SECONDS, "seconds").symbol(TimeUnit.MICROSECONDS, "micros").build();
assertEquals("00|01_hours|02|03_seconds|000|000_micros|000", df.formatMillis(HOURS.toMillis(1) 
	+ MINUTES.toMillis(2) + SECONDS.toMillis(3)));
```
 

## Integration
Since durationformatter is hosted on maven central Maven users can easily use the library  by adding the dependency to their pom.xml:

```xml
<dependency>
	<groupId>com.github.pfichtner</groupId>
	<artifactId>durationformatter</artifactId>
	<version>XXX</version>
</dependency>
```

Non-maven users can copy the artifact(s) directly from maven central into their local workspace<br>
http://central.maven.org/maven2/com/github/pfichtner/durationformatter/

## License
Copyright 2012-2014 Peter Fichtner - Released under the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0.html)
