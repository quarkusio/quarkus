<a name="duration-note-anchor"></a>

> [!NOTE]
> ### About the Duration format
> 
> To write duration values, use the standard `java.time.Duration` format.
> See the [Duration#parse()](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html#parse(java.lang.CharSequence)) Java API documentation] for more information.
> 
> You can also use a simplified format, starting with a number:
> 
> * If the value is only a number, it represents time in seconds.
> * If the value is a number followed by `ms`, it represents time in milliseconds.
> 
> In other cases, the simplified format is translated to the `java.time.Duration` format for parsing:
> 
> * If the value is a number followed by `h`, `m`, or `s`, it is prefixed with `PT`.
> * If the value is a number followed by `d`, it is prefixed with `P`.
