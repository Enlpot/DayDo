/*
 * Copyright (C) 2026  Enlpot
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.enlpot.daydo.shared.ui.habit

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.GolfCourse
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.PregnantWoman
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Rowing
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Skateboarding
import androidx.compose.material.icons.filled.SmokeFree
import androidx.compose.material.icons.filled.Snowboarding
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.SportsHandball
import androidx.compose.material.icons.filled.SportsHockey
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material.icons.filled.SportsRugby
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material.icons.filled.Yard
import androidx.compose.ui.graphics.vector.ImageVector

/** 内置习惯单色图标集合（Material Icons，100+ 个），key 持久化到数据库 */
val HABIT_ICONS: List<String> =
    listOf(
            // 常用
            "star",
            "fitness_center",
            "directions_run",
            "self_improvement",
            "menu_book",
            "local_library",
            "psychology",
            "spa",
            "water_drop",
            "restaurant",
            // 作息
            "local_cafe",
            "nightlight",
            "bedtime",
            "monitor_weight",
            "smoke_free",
            "wb_sunny",
            "medication",
            "schedule",
            "alarm",
            "bed",
            // 运动
            "hiking",
            "directions_bike",
            "waves",
            "golf_course",
            "skateboarding",
            "sports_esports",
            "snowboarding",
            "surfing",
            "rowing",
            "pool",
            "sports_martial_arts",
            "sports_mma",
            "sports_gymnastics",
            "sports_handball",
            "sports_volleyball",
            "sports_baseball",
            "sports_cricket",
            "sports_basketball",
            "sports_soccer",
            "sports_tennis",
            "sports_hockey",
            "sports_rugby",
            "sports_motorsports",
            "directions_run",
            // 健康
            "health_and_safety",
            "monitor_heart",
            "vaccines",
            "bloodtype",
            "favorite",
            "thumb_up",
            "shield",
            "air",
            "ac_unit",
            "whatshot",
            "flare",
            "lightbulb",
            "eco",
            // 学习
            "school",
            "flag",
            "emoji_events",
            "trending_up",
            "history",
            "checklist",
            "fact_check",
            "code",
            "computer",
            "language",
            "public",
            "explore",
            "architecture",
            // 爱好
            "music_note",
            "piano",
            "headphones",
            "mic",
            "videocam",
            "movie",
            "photo_camera",
            "palette",
            "brush",
            "celebration",
            // 家务
            "cleaning_services",
            "home",
            "kitchen",
            "yard",
            "grass",
            "forest",
            "landscape",
            "beach_access",
            "storefront",
            // 理财
            "attach_money",
            "savings",
            "account_balance",
            "shopping_cart",
            "local_grocery_store",
            "fastfood",
            "icecream",
            "cake",
            "wine_bar",
            "local_drink",
            // 社交
            "handshake",
            "group",
            "people",
            "family_restroom",
            "child_care",
            "pregnant_woman",
            "elderly",
            "pets",
            "volunteer_activism",
            "waving_hand",
            "directions_walk",
            "accessibility",
            "directions_car",
            "flight",
        )
        .distinct()

/** 按 key 取单色图标，未知 key 回退星形 */
fun habitIcon(name: String): ImageVector =
    when (name) {
        "star" -> Icons.Filled.Star
        "fitness_center" -> Icons.Filled.FitnessCenter
        "directions_run" -> Icons.Filled.DirectionsRun
        "self_improvement" -> Icons.Filled.SelfImprovement
        "menu_book" -> Icons.Filled.MenuBook
        "local_library" -> Icons.Filled.LocalLibrary
        "psychology" -> Icons.Filled.Psychology
        "spa" -> Icons.Filled.Spa
        "water_drop" -> Icons.Filled.WaterDrop
        "restaurant" -> Icons.Filled.Restaurant
        "local_cafe" -> Icons.Filled.LocalCafe
        "nightlight" -> Icons.Filled.Nightlight
        "bedtime" -> Icons.Filled.Bedtime
        "monitor_weight" -> Icons.Filled.MonitorWeight
        "smoke_free" -> Icons.Filled.SmokeFree
        "wb_sunny" -> Icons.Filled.WbSunny
        "medication" -> Icons.Filled.Medication
        "schedule" -> Icons.Filled.Schedule
        "alarm" -> Icons.Filled.Alarm
        "bed" -> Icons.Filled.Bed
        "hiking" -> Icons.Filled.Hiking
        "directions_bike" -> Icons.Filled.DirectionsBike
        "waves" -> Icons.Filled.Waves
        "golf_course" -> Icons.Filled.GolfCourse
        "skateboarding" -> Icons.Filled.Skateboarding
        "sports_esports" -> Icons.Filled.SportsEsports
        "snowboarding" -> Icons.Filled.Snowboarding
        "surfing" -> Icons.Filled.Surfing
        "rowing" -> Icons.Filled.Rowing
        "pool" -> Icons.Filled.Pool
        "sports_martial_arts" -> Icons.Filled.SportsMartialArts
        "sports_mma" -> Icons.Filled.SportsMma
        "sports_gymnastics" -> Icons.Filled.SportsGymnastics
        "sports_handball" -> Icons.Filled.SportsHandball
        "sports_volleyball" -> Icons.Filled.SportsVolleyball
        "sports_baseball" -> Icons.Filled.SportsBaseball
        "sports_cricket" -> Icons.Filled.SportsCricket
        "sports_basketball" -> Icons.Filled.SportsBasketball
        "sports_soccer" -> Icons.Filled.SportsSoccer
        "sports_tennis" -> Icons.Filled.SportsTennis
        "sports_hockey" -> Icons.Filled.SportsHockey
        "sports_rugby" -> Icons.Filled.SportsRugby
        "sports_motorsports" -> Icons.Filled.SportsMotorsports
        "health_and_safety" -> Icons.Filled.HealthAndSafety
        "monitor_heart" -> Icons.Filled.MonitorHeart
        "vaccines" -> Icons.Filled.Vaccines
        "bloodtype" -> Icons.Filled.Bloodtype
        "favorite" -> Icons.Filled.Favorite
        "thumb_up" -> Icons.Filled.ThumbUp
        "shield" -> Icons.Filled.Shield
        "air" -> Icons.Filled.Air
        "ac_unit" -> Icons.Filled.AcUnit
        "whatshot" -> Icons.Filled.Whatshot
        "flare" -> Icons.Filled.Flare
        "lightbulb" -> Icons.Filled.Lightbulb
        "eco" -> Icons.Filled.Eco
        "school" -> Icons.Filled.School
        "flag" -> Icons.Filled.Flag
        "emoji_events" -> Icons.Filled.EmojiEvents
        "trending_up" -> Icons.Filled.TrendingUp
        "history" -> Icons.Filled.History
        "checklist" -> Icons.Filled.Checklist
        "fact_check" -> Icons.Filled.FactCheck
        "code" -> Icons.Filled.Code
        "computer" -> Icons.Filled.Computer
        "language" -> Icons.Filled.Language
        "public" -> Icons.Filled.Public
        "explore" -> Icons.Filled.Explore
        "architecture" -> Icons.Filled.Architecture
        "music_note" -> Icons.Filled.MusicNote
        "piano" -> Icons.Filled.Piano
        "headphones" -> Icons.Filled.Headphones
        "mic" -> Icons.Filled.Mic
        "videocam" -> Icons.Filled.Videocam
        "movie" -> Icons.Filled.Movie
        "photo_camera" -> Icons.Filled.PhotoCamera
        "palette" -> Icons.Filled.Palette
        "brush" -> Icons.Filled.Brush
        "celebration" -> Icons.Filled.Celebration
        "cleaning_services" -> Icons.Filled.CleaningServices
        "home" -> Icons.Filled.Home
        "kitchen" -> Icons.Filled.Kitchen
        "yard" -> Icons.Filled.Yard
        "grass" -> Icons.Filled.Grass
        "forest" -> Icons.Filled.Forest
        "landscape" -> Icons.Filled.Landscape
        "beach_access" -> Icons.Filled.BeachAccess
        "storefront" -> Icons.Filled.Storefront
        "attach_money" -> Icons.Filled.AttachMoney
        "savings" -> Icons.Filled.Savings
        "account_balance" -> Icons.Filled.AccountBalance
        "shopping_cart" -> Icons.Filled.ShoppingCart
        "local_grocery_store" -> Icons.Filled.LocalGroceryStore
        "fastfood" -> Icons.Filled.Fastfood
        "icecream" -> Icons.Filled.Icecream
        "cake" -> Icons.Filled.Cake
        "wine_bar" -> Icons.Filled.WineBar
        "local_drink" -> Icons.Filled.LocalDrink
        "handshake" -> Icons.Filled.Handshake
        "group" -> Icons.Filled.Group
        "people" -> Icons.Filled.People
        "family_restroom" -> Icons.Filled.FamilyRestroom
        "child_care" -> Icons.Filled.ChildCare
        "pregnant_woman" -> Icons.Filled.PregnantWoman
        "elderly" -> Icons.Filled.Elderly
        "pets" -> Icons.Filled.Pets
        "volunteer_activism" -> Icons.Filled.VolunteerActivism
        "waving_hand" -> Icons.Filled.WavingHand
        "directions_walk" -> Icons.Filled.DirectionsWalk
        "accessibility" -> Icons.Filled.Accessibility
        "directions_car" -> Icons.Filled.DirectionsCar
        "flight" -> Icons.Filled.Flight
        else -> Icons.Filled.Star
    }
