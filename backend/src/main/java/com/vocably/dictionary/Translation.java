package com.vocably.dictionary;

import java.util.List;

/** Tradução principal + outras opções sugeridas pela memória de tradução. */
public record Translation(String text, List<String> alternatives) {
}
