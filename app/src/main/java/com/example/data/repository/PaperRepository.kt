package com.example.data.repository

import com.example.data.local.PaperDao
import com.example.data.model.DigitizedPaper
import kotlinx.coroutines.flow.Flow

class PaperRepository(private val paperDao: PaperDao) {
    val allPapers: Flow<List<DigitizedPaper>> = paperDao.getAllPapers()

    suspend fun getPaperById(id: Int): DigitizedPaper? {
        return paperDao.getPaperById(id)
    }

    suspend fun insertPaper(paper: DigitizedPaper): Long {
        return paperDao.insertPaper(paper)
    }

    suspend fun updatePaper(paper: DigitizedPaper) {
        paperDao.updatePaper(paper)
    }

    suspend fun deletePaper(paper: DigitizedPaper) {
        paperDao.deletePaper(paper)
    }

    suspend fun deletePaperById(id: Int) {
        paperDao.deletePaperById(id)
    }
}
