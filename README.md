durationformatter
=================

Java Library minimal in size and without overhead just for formatting durations

Intention
---------

Because I could not find the 5th time i searched for an artifact just for formatting 
durations to strings, I started to write my own. This class should be flexible 
enough to cover all possible needs.

Usage
-----

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
DurationFormatter df = new Builder().maximum(TimeUnit.DAYS).minimum(TimeUnit.NANOSECONDS).separator("|||||")
	.valueSymbolSeparator("xXx").symbol(HOURS, "<<H>>").symbol(SECONDS, "<<S>>").build();
assertEquals("00|||||01xXx<<H>>|||||02|||||03xXx<<S>>|||||00|||||00|||||00", df.formatMillis(HOURS.toMillis(1) 
	+ MINUTES.toMillis(2) + SECONDS.toMillis(3)));
```
 

Integration
-----------

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

[![Build Status](https://buildhive.cloudbees.com/job/pfichtner/job/durationformatter/badge/icon)](https://buildhive.cloudbees.com/job/pfichtner/job/durationformatter/)
