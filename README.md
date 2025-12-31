# Собрать

```bash
mvn compile
```

# Запустить
```
M2_REPO="$HOME/.m2/repository"

java -Xss16m -classpath \
    target/classes:\
    $M2_REPO/org/graalvm/truffle/truffle-api/25.0.0/truffle-api-25.0.0.jar:\
    $M2_REPO/org/graalvm/sdk/collections/25.0.0/collections-25.0.0.jar:\
    $M2_REPO/org/graalvm/sdk/nativeimage/25.0.0/nativeimage-25.0.0.jar:\
    $M2_REPO/org/graalvm/sdk/word/25.0.0/word-25.0.0.jar:\
    $M2_REPO/org/graalvm/truffle/truffle-runtime/25.0.0/truffle-runtime-25.0.0.jar:\
    $M2_REPO/org/graalvm/sdk/jniutils/25.0.0/jniutils-25.0.0.jar:\
    $M2_REPO/org/graalvm/truffle/truffle-compiler/25.0.0/truffle-compiler-25.0.0.jar:\
    $M2_REPO/org/antlr/antlr4-runtime/4.13.2/antlr4-runtime-4.13.2.jar:\
    $M2_REPO/org/graalvm/polyglot/polyglot/25.0.0/polyglot-25.0.0.jar \
    space.elteammate.lama.PolyglotLamaMain \
    -Xss32m
    Lama/performance/Sort.lama
```
`main` лежит в [src/main/java/space/elteammate/lama/PolyglotLamaMain.java](src/main/java/space/elteammate/lama/PolyglotLamaMain.java).

# Запустить тесты
```bash
mvn test
```

# Производительность

Я немного поменял Sort.lama, чтобы был эффект от сортировки списка.
```ocaml
fun checkSorted (l) {
  case l of
    x : z@(y : tl) ->
      if x <= y
      then checkSorted (z)
      else false
      fi
  | _ -> true
  esac
}

fun bubbleSort (l) {
  fun inner (l) {
    case l of
      x : z@(y : tl) ->
       if x > y
       then [true, y : inner (x : tl) [1]]
       else case inner (z) of [f, z] -> [f, x : z] esac
       fi
    | _ -> [false, l]
    esac
  }

  fun rec (l) {
    case inner (l) of
      [true , l] -> rec (l)
    | [false, l] -> l
    esac
  }

  rec (l)
}

fun generate (n) {
  if n then n : generate (n-1) else {} fi
}

let sortedList = bubbleSort (generate (10000)) in
  if checkSorted (sortedList)
  then write(1)
  else write(0)
  fi
```
Просто, чтобы компилятор не оптимизировал слишком много.

`ImperativeBubbleSort.java` и `FunctionalBubbleSort.java` -
это бейзлайны, написанные на Java.

```bash
# Дз 4
$ ../vm-course_hw-04/cmake-build/hw profile Sort.bc
Analysis time: 0.013255ms
Execution time: 89.722000s

________________________________________________________
Executed in   90.03 secs    fish           external
   usr time   86.20 secs  241.00 micros   86.20 secs
   sys time    3.83 secs  128.00 micros    3.83 secs

# Дз 5
$ time java -classpath <--snip ---> -Xss32m space.elteammate.lama.PolyglotLamaMain Lama/performance/Sort.lama
WARNING: A restricted method in java.lang.System has been called
WARNING: java.lang.System::load has been called by com.oracle.truffle.polyglot.JDKSupport in an unnamed module (file:/home/elt/.m2/repository/org/graalvm/truffle/truffle-api/25.0.0/truffle-api-25.0.0.jar)
WARNING: Use --enable-native-access=ALL-UNNAMED to avoid a warning for callers in this module
WARNING: Restricted methods will be blocked in a future release unless native access is enabled

WARNING: A terminally deprecated method in sun.misc.Unsafe has been called
WARNING: sun.misc.Unsafe::objectFieldOffset has been called by com.oracle.truffle.runtime.hotspot.HotSpotTruffleRuntime (file:/home/elt/.m2/repository/org/graalvm/truffle/truffle-runtime/25.0.0/truffle-runtime-25.0.0.jar)
WARNING: Please consider reporting this to the maintainers of class com.oracle.truffle.runtime.hotspot.HotSpotTruffleRuntime
WARNING: sun.misc.Unsafe::objectFieldOffset will be removed in a future release
== running on org.graalvm.polyglot.Engine@22295ec4
1

________________________________________________________
Executed in    7.54 secs    fish           external
   usr time   23.80 secs    0.00 micros   23.80 secs
   sys time    3.91 secs  953.00 micros    3.91 secs

# Lamac нативный
$ lamac Lama/performance/Sort.lama && time ./Sort
________________________________________________________
Executed in   60.57 secs    fish           external
   usr time   56.91 secs  232.00 micros   56.91 secs
   sys time    3.61 secs  116.00 micros    3.61 secs
   
# Итеративный baseline
$ time java -Xss32m ImperativeBubbleSort.java
________________________________________________________
Executed in    1.62 secs    fish           external
   usr time    1.62 secs    0.00 millis    1.62 secs
   sys time    0.82 secs    1.72 millis    0.82 secs

# Функциональный baseline
$ time java -Xss32m FunctionalBubbleSort.java                                                                               (base) 2619ms  Ср 31 дек 2025 20:12:46

________________________________________________________
Executed in    2.57 secs    fish           external
   usr time    3.07 secs  353.00 micros    3.07 secs
   sys time    0.66 secs  451.00 micros    0.66 secs
```