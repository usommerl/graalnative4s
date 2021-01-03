# Using UPX compressed executables

The size of the binary that was created with GraalVM `native-image` can be reduced even further by using [UPX][upx]. Compression with UPX has a significant impact on the file size but also incurs a penalty with regards to startup performance and memory consumption. In my test setup, the executable that was compressed with option `--best` was 48 MB smaller than the original file. However, compared to its uncompressed counterpart the startup time of the compressed executable was approximately 300 milliseconds slower. Please see the following sections for more detailed test results.

### Size comparison

I used the uncompressed binary from release v0.3.1 and compressed it with all available UPX compression options:

| Binary | Size [byte] |
|:---|---:|
| v0.3.1-no-upx          | 67.712.920 |
| v0.3.1-upx-1           | 26.456.312 |
| v0.3.1-upx-2           | 25.614.824 |
| v0.3.1-upx-3           | 25.240.056 |
| v0.3.1-upx-4           | 23.903.052 |
| v0.3.1-upx-5           | 22.732.760 |
| v0.3.1-upx-6           | 22.413.428 |
| v0.3.1-upx-7           | 21.868.336 |
| v0.3.1-upx-8           | 21.603.612 |
| v0.3.1-upx-9           | 21.335.948 |
| v0.3.1-upx-best        | 18.757.440 |
| v0.3.1-upx-brute       | 13.582.308 |
| v0.3.1-upx-ultra-brute | 13.540.112 |

### Memory comparison

In my test setup, the memory consumption of compressed executables was approximately 70 MB higher because the binary needs to be decompressed on the fly before it is executed. All test results were acquired via this [script][script2].

| Binary | Memory [bytes] |
|:---|---:|
| v0.3.1-no-upx             |   66.242.560 |
| v0.3.1-upx-1              |  140.781.568 |
| v0.3.1-upx-2              |  140.781.568 |
| v0.3.1-upx-3              |  140.797.952 |
| v0.3.1-upx-4              |  140.773.376 |
| v0.3.1-upx-5              |  140.797.952 |
| v0.3.1-upx-6              |  140.773.376 |
| v0.3.1-upx-7              |  140.781.568 |
| v0.3.1-upx-8              |  140.789.760 |
| v0.3.1-upx-9              |  140.781.568 |
| v0.3.1-upx-best           |  140.756.992 |
| v0.3.1-upx-brute          |  140.806.144 |
| v0.3.1-upx-ultra-brute    |  141.182.976 |

### Startup performance

I used [hyperfine][hyperfine] in conjunction with [a shell script][script1] to measure the time to the first response of the server executable. The tests were run on a fairly outdated Lenovo X220 laptop (Intel Core i7-2640M, 16 GB RAM). This is the hyperfine command line that measured the results further down below:

```
hyperfine -r 50 -L compression no-upx,upx-best,upx-9,upx-8,upx-7,upx-6,upx-5,upx-4,upx-3,upx-2,upx-1,upx-brute,upx-ultra-brute './time-to-first-response.sh -s v0.3.1-{compression} -c "curl localhost:8080/hello"'
```

| Command | Mean [ms] | Min [ms] | Max [ms] | Relative |
|:---|---:|---:|---:|---:|
| `./time-to-first-response.sh -s v0.3.1-no-upx -c "curl localhost:8080/hello"`          | 41.5 ± 10.7   | 22.8   | 63.6   | 1.00         |
| `./time-to-first-response.sh -s v0.3.1-upx-1 -c "curl localhost:8080/hello"`           | 375.9 ± 22.4  | 328.3  | 422.7  | 9.05 ± 2.38  |
| `./time-to-first-response.sh -s v0.3.1-upx-2 -c "curl localhost:8080/hello"`           | 372.4 ± 23.9  | 334.3  | 449.8  | 8.96 ± 2.37  |
| `./time-to-first-response.sh -s v0.3.1-upx-3 -c "curl localhost:8080/hello"`           | 367.8 ± 17.2  | 329.1  | 405.1  | 8.85 ± 2.31  |
| `./time-to-first-response.sh -s v0.3.1-upx-4 -c "curl localhost:8080/hello"`           | 355.9 ± 18.5  | 320.9  | 400.1  | 8.57 ± 2.24  |
| `./time-to-first-response.sh -s v0.3.1-upx-5 -c "curl localhost:8080/hello"`           | 336.3 ± 19.7  | 299.7  | 375.9  | 8.10 ± 2.13  |
| `./time-to-first-response.sh -s v0.3.1-upx-6 -c "curl localhost:8080/hello"`           | 336.0 ± 19.8  | 295.4  | 368.1  | 8.09 ± 2.13  |
| `./time-to-first-response.sh -s v0.3.1-upx-7 -c "curl localhost:8080/hello"`           | 326.3 ± 19.9  | 284.8  | 360.6  | 7.86 ± 2.07  |
| `./time-to-first-response.sh -s v0.3.1-upx-8 -c "curl localhost:8080/hello"`           | 316.5 ± 22.0  | 283.2  | 368.3  | 7.62 ± 2.02  |
| `./time-to-first-response.sh -s v0.3.1-upx-9 -c "curl localhost:8080/hello"`           | 315.2 ± 19.6  | 273.5  | 358.3  | 7.59 ± 2.00  |
| `./time-to-first-response.sh -s v0.3.1-upx-best -c "curl localhost:8080/hello"`        | 330.7 ± 19.7  | 285.4  | 375.8  | 7.96 ± 2.10  |
| `./time-to-first-response.sh -s v0.3.1-upx-brute -c "curl localhost:8080/hello"`       | 1144.4 ± 26.3 | 1098.4 | 1236.2 | 27.55 ± 7.09 |
| `./time-to-first-response.sh -s v0.3.1-upx-ultra-brute -c "curl localhost:8080/hello"` | 1198.6 ± 22.4 | 1155.3 | 1246.2 | 28.85 ± 7.42 |


[hyperfine]: https://github.com/sharkdp/hyperfine
[upx]: https://github.com/upx/upx
[script1]: ./time-to-first-response.sh
[script2]: ./memory-consumption.sh
