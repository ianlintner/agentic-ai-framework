package com.agenticai.demo

import zio.*
import scala.concurrent.ExecutionContext

/** This file demonstrates Scala 3 specific features that are impossible to compile with Scala 2.x
  * It serves as a proof that our project is using Scala 3
  */

// Top-level functions (Scala 3 only)
def topLevelFunction(x: Int): Int = x * 2

// Top-level given instance (Scala 3 only)
given intOrdering: Ordering[Int] = Ordering.Int

// Native enum (Scala 3 only)
enum Color:
  case Red
  case Green
  case Blue
  case Custom(hex: String)

  // Methods in enum (Scala 3 only)
  def description: String = this match
    case Red         => "The color red"
    case Green       => "The color green"
    case Blue        => "The color blue"
    case Custom(hex) => s"Custom color with hex code $hex"

end Color

// Union types (Scala 3 only)
type IntOrString = Int | String

// Intersection types (Scala 3 only)
trait A
trait B
type AandB = A & B

// Match types (Scala 3 only)
type Elem[X] = X match
  case String      => Char
  case Array[t]    => t
  case Iterable[t] => t

// Extension methods (Scala 3 only)
extension (s: String)
  def exclaim: String = s + "!"
  def double: String  = s + s

// Opaque type aliases (Scala 3 only)
object Temperature:
  opaque type Celsius = Double

  // Companion methods for opaque types
  object Celsius:
    def apply(d: Double): Celsius                   = d
    extension (c: Celsius) def toCelsius: Double    = c
    extension (c: Celsius) def toFahrenheit: Double = c * 9 / 5 + 32

// Context functions (Scala 3 only)
type Contextual[T] = ExecutionContext ?=> T

// New control syntax (Scala 3 only)
def conditionalValue(test: Boolean) =
  if test then "then branch"
  else "else branch"

// Indentation-based syntax (Scala 3 only)
def processItems(items: List[Int]): List[Int] =
  if items.isEmpty then Nil
  else
    val processed = for
      item <- items
      doubled = item * 2
      if doubled > 10
    yield doubled
    processed

// Export clauses (Scala 3 only)
class Container:
  def method1(): String = "method1"
  def method2(): String = "method2"

class Exporter:
  private val container = Container()
  export container.{method1, method2}

// Type lambdas (Scala 3 only)
type MapTo[T] = [X] =>> Map[X, T]

// Main ZIO app using Scala 3 features
object Scala3OnlyApp extends ZIOAppDefault:

  // Significant whitespace and braces-free syntax (Scala 3 only)
  def run =
    for
      _ <- Console.printLine("This app can ONLY be compiled with Scala 3!")
      _ <- Console.printLine("It uses features that don't exist in Scala 2.x")
      // Use enum values
      colors = List(Color.Red, Color.Green, Color.Blue, Color.Custom("#FF00FF"))
      _ <- Console.printLine(s"Enum values: ${colors.map(_.toString).mkString(", ")}")
      redDescription = Color.Red.description
      _         <- Console.printLine(s"Red description: $redDescription")
      exclaimed <- ZIO.succeed("Hello Scala 3".exclaim)
      _         <- Console.printLine(exclaimed)
      tempC = Temperature.Celsius(100)
      tempF = tempC.toFahrenheit
      _ <- Console.printLine(s"100°C = $tempF°F")
      result = processItems(List(1, 10, 5, 12))
      _ <- Console.printLine(s"Processed items: $result")
    yield ()
