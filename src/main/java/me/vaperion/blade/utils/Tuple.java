package me.vaperion.blade.utils;

import lombok.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class Tuple<A, B> {
    private A left;
    private B right;
}