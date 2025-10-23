package com.example.mascotasapp.data.model

data class Pet(
    val pet_id: String,
    val user_id: String,
    val name: String,
    val imageUrl: String,
    val species: String,
    val sex: String,
    val breed: String,
    val date_of_birth: String,
    val weight_kg: String,
    val color: String,
    val created_at: String
)
